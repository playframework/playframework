/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.forkrun

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{ Config, ConfigFactory }
import java.io.File
import java.lang.{ Runtime, Thread }
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeoutException
import play.forkrun.protocol.{ ForkConfig, Serializers }
import play.runsupport.Reloader.{ CompileResult, PlayDevServer }
import play.runsupport.{ Colors, LoggerProxy, RunHook, FileWatchService, Reloader }
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Success, Failure, Properties }

object ForkRun {
  case object Reload
  case object Close

  private val running = new AtomicBoolean(true)

  def main(args: Array[String]): Unit = {
    val baseDirectory = args(0)
    val configKey = args(1)
    val runArgs = args.drop(2)

    val logLevel = Properties.propOrElse("fork.run.log.level", "info")
    val logEvents = Properties.propOrFalse("fork.run.log.events")

    val log = Logger(logLevel)
    val system = ActorSystem("play-fork-run", akkaNoLogging)
    val sbt = system.actorOf(SbtClient.props(new File(baseDirectory), log, logEvents), "sbt")
    val forkRun = system.actorOf(props(sbt, configKey, runArgs, log), "fork-run")

    log.info("Setting up Play fork run ... (use Ctrl+D to cancel)")
    registerShutdownHook(log, system, forkRun)
    waitForStop()

    doShutdown(log, system, forkRun)
  }

  def doShutdown(log: Logger, system: ActorSystem, forkRun: ActorRef): Unit = {
    if (running.compareAndSet(true, false)) {
      log.info("Stopping Play fork run ...")
      forkRun ! Close
      try system.awaitTermination(30.seconds)
      catch { case _: TimeoutException => System.exit(1) }
    } else {
      log.info("Play fork run already stopped ...")
    }
  }

  def registerShutdownHook(log: Logger, system: ActorSystem, forkRun: ActorRef): Unit = {
    Runtime.getRuntime().addShutdownHook(new Thread {
      override def run(): Unit = {
        log.info("JVM exiting, shutting down Play fork run ...")
        doShutdown(log, system, forkRun)
      }
    })
  }

  def startServer(config: ForkConfig, args: Seq[String], notifyStart: InetSocketAddress => Unit, reloadCompile: () => CompileResult, log: Logger): PlayDevServer = {
    val notifyStartHook = new RunHook {
      override def afterStarted(address: InetSocketAddress): Unit = notifyStart(address)
    }

    val watchService = config.watchService match {
      case ForkConfig.DefaultWatchService => FileWatchService.defaultWatchService(config.targetDirectory, config.pollInterval, log)
      case ForkConfig.JDK7WatchService => FileWatchService.jdk7(log)
      case ForkConfig.JNotifyWatchService => FileWatchService.jnotify(config.targetDirectory)
      case ForkConfig.PollingWatchService(pollInterval) => FileWatchService.sbt(pollInterval)
    }

    val runSbtTask = (s: String) => throw new UnsupportedOperationException("BuildLink.runTask is not supported in fork run")

    val server = Reloader.startDevMode(
      runHooks = Seq(notifyStartHook),
      javaOptions = config.javaOptions,
      dependencyClasspath = config.dependencyClasspath,
      dependencyClassLoader = Reloader.createURLClassLoader,
      reloadCompile = reloadCompile,
      reloaderClassLoader = Reloader.createDelegatedResourcesClassLoader,
      assetsClassLoader = Reloader.assetsClassLoader(config.allAssets),
      commonClassLoader = Reloader.commonClassLoader(config.dependencyClasspath),
      monitoredFiles = config.monitoredFiles,
      fileWatchService = watchService,
      docsClasspath = config.docsClasspath,
      docsJar = config.docsJar,
      defaultHttpPort = config.defaultHttpPort,
      defaultHttpAddress = config.defaultHttpAddress,
      projectPath = config.projectDirectory,
      devSettings = config.devSettings,
      args = args,
      runSbtTask = runSbtTask,
      mainClassName = config.mainClass
    )

    println()
    println(Colors.green("(Server started, use Ctrl+D to stop and go back to the console...)"))
    println()

    server
  }

  def sendStart(sbt: ActorRef, config: ForkConfig, args: Seq[String]): InetSocketAddress => Unit = { address =>
    val url = serverUrl(args, config.defaultHttpPort, config.defaultHttpAddress, address)
    sbt ! SbtClient.Execute(s"${config.notifyKey} $url")
  }

  // reparse args to support https urls
  def serverUrl(args: Seq[String], defaultHttpPort: Int, defaultHttpAddress: String, address: InetSocketAddress): String = {
    val (properties, httpPort, httpsPort, httpAddress) = Reloader.filterArgs(args, defaultHttpPort, defaultHttpAddress)
    val host = if (httpAddress == "0.0.0.0") "localhost" else httpAddress
    if (httpPort.isDefined) s"http://$host:${httpPort.get}"
    else if (httpsPort.isDefined) s"https://$host:${httpsPort.get}"
    else s"http://$host:${address.getPort}"
  }

  def askForReload(actor: ActorRef)(implicit timeout: Timeout): () => CompileResult = () => {
    val future = (actor ? ForkRun.Reload).mapTo[CompileResult]
    Await.result(future, timeout.duration)
  }

  def waitForStop(): Unit = {
    System.in.read() match {
      case -1 | 4 => // exit on EOF or EOT/Ctrl-D
      case _ => waitForStop()
    }
  }

  def cancel(): Unit = {
    // close stdin in case we're in waitForStop
    System.in.close()
  }

  def akkaNoLogging: Config = {
    ConfigFactory.parseString("""
      akka.stdout-loglevel = "OFF"
      akka.loglevel = "OFF"
    """)
  }

  def props(sbt: ActorRef, configKey: String, args: Seq[String], log: Logger): Props = Props(new ForkRun(sbt, configKey, args, log))
}

class ForkRun(sbt: ActorRef, configKey: String, args: Seq[String], log: Logger) extends Actor {
  import SbtClient._
  import Serializers._

  def receive: Receive = setup

  def setup: Receive = {
    sbt ! Request(configKey, self)
    settingUp
  }

  def settingUp: Receive = {
    case Response(`configKey`, result) =>
      result.result[ForkConfig] match {
        case Success(config) => run(config)
        case Failure(error) => fail(error)
      }
    case Failed(error) => fail(error)
    case ForkRun.Close => shutdown()
  }

  def run(config: ForkConfig): Unit = {
    try {
      val notifyStart = ForkRun.sendStart(sbt, config, args)
      val reloadCompile = ForkRun.askForReload(self)(Timeout(config.compileTimeout.millis))
      val server = ForkRun.startServer(config, args, notifyStart, reloadCompile, log)
      context become running(server, config.reloadKey)
    } catch {
      case e: Exception => fail(e)
    }
  }

  def running(server: PlayDevServer, reloadKey: String): Receive = {
    case ForkRun.Reload => reload(server, reloadKey)
    case ForkRun.Close => close(server)
  }

  def reload(server: PlayDevServer, reloadKey: String): Unit = {
    sbt ! Request(reloadKey, self)
    val replyTo = sender()
    context become reloading(server, reloadKey, replyTo)
  }

  def reloading(server: PlayDevServer, reloadKey: String, replyTo: ActorRef): Receive = {
    case Response(`reloadKey`, result) =>
      result.result[CompileResult] match {
        case Success(result) =>
          replyTo ! result
          context become running(server, reloadKey)
        case Failure(error) => fail(error)
      }
    case ForkRun.Close => close(server)
  }

  def close(server: PlayDevServer): Unit = {
    server.close()
    shutdown()
  }

  def fail(cause: Throwable): Unit = {
    log.error("Play fork run has failed due to:")
    log.trace(cause)
    shutdown()
    ForkRun.cancel()
  }

  def shutdown(): Unit = {
    sbt ! Shutdown
    context become shuttingDown
  }

  def shuttingDown: Receive = {
    case _ => // ignore messages while shutting down
  }
}
