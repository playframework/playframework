/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.io._
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardOpenOption

import scala.concurrent.Future

import akka.Done
import akka.actor.CoordinatedShutdown

import play.api._

/**
 * Used to start servers in 'prod' mode, the mode that is
 * used in production. The application is loaded and started
 * immediately.
 */
object ProdServerStart {

  /**
   * Start a prod mode server from the command line.
   */
  def main(args: Array[String]): Unit = start(new RealServerProcess(args))

  /**
   * Starts a Play server and application for the given process. The settings
   * for the server are based on values passed on the command line and in
   * various system properties. Crash out by exiting the given process if there
   * are any problems.
   *
   * @param process The process (real or abstract) to use for starting the server.
   */
  def start(process: ServerProcess): ReloadableServer = {
    try {
      // Read settings
      val config: ServerConfig = readServerConfigSettings(process)

      // Create a PID file before we do any real work
      val pidFile = createPidFile(process, config.configuration)

      try {
        // Start the application
        val application: Application = {
          val environment = Environment(config.rootDir, process.classLoader, Mode.Prod)
          val context     = ApplicationLoader.Context.create(environment)
          val loader      = ApplicationLoader(context)
          loader.load(context)
        }
        Play.start(application)

        // Start the server
        val serverProvider = ServerProvider.fromConfiguration(process.classLoader, config.configuration)
        val server         = serverProvider.createServer(config, application)

        val phase = CoordinatedShutdown.PhaseBeforeActorSystemTerminate
        application.coordinatedShutdown.addTask(phase, "remove-pid-file") { () =>
          // Must delete the PID file after stopping the server not before...
          // In case of unclean shutdown or failure, leave the PID file there!
          pidFile.foreach(_.delete())
          assert(pidFile.forall(!_.exists), "PID file should not exist!")
          Future.successful(Done)
        }

        process.addShutdownHook {
          // Only run server stop if the shutdown reason is not defined. That means the
          // process received a SIGTERM (or other acceptable signal) instead of being
          // stopped because of CoordinatedShutdown, for example when downing a cluster.
          // The reason for that is we want to avoid calling coordinated shutdown from
          // inside a JVM shutdown hook if the trigger of the JVM shutdown hook was
          // coordinated shutdown.
          if (application.coordinatedShutdown.shutdownReason().isEmpty) {
            server.stop()
          }
        }

        server
      } catch {
        case e: Throwable =>
          // Clean up pidfile when the server fails to start
          pidFile.foreach(_.delete())
          throw e
      }
    } catch {
      case ServerStartException(message, cause) => process.exit(message, cause)
      case e: Throwable                         => process.exit("Oops, cannot start the server.", Some(e))
    }
  }

  /**
   * Read the server config from the current process's command line args and system properties.
   */
  def readServerConfigSettings(process: ServerProcess): ServerConfig = {
    val configuration: Configuration = {
      val rootDirArg    = process.args.headOption.map(new File(_))
      val rootDirConfig = rootDirArg.fold(Map.empty[String, String])(ServerConfig.rootDirConfig(_))
      Configuration.load(process.classLoader, process.properties, rootDirConfig, true)
    }

    val rootDir: File = {
      val path = configuration
        .getOptional[String]("play.server.dir")
        .getOrElse(throw ServerStartException("No root server path supplied"))
      val file = new File(path)
      if (!file.isDirectory)
        throw ServerStartException(s"Bad root server path: $path")
      file
    }

    def parsePort(portType: String): Option[Int] = {
      configuration.getOptional[String](s"play.server.$portType.port").filter(_ != "disabled").map { str =>
        try Integer.parseInt(str)
        catch {
          case _: NumberFormatException =>
            throw ServerStartException(s"Invalid ${portType.toUpperCase} port: $str")
        }
      }
    }

    val httpPort  = parsePort("http")
    val httpsPort = parsePort("https")
    val address   = configuration.getOptional[String]("play.server.http.address").getOrElse("0.0.0.0")

    if (httpPort.orElse(httpsPort).isEmpty)
      throw ServerStartException("Must provide either an HTTP or HTTPS port")

    ServerConfig(rootDir, httpPort, httpsPort, address, Mode.Prod, process.properties, configuration)
  }

  /**
   * Create a pid file for the current process.
   */
  def createPidFile(process: ServerProcess, configuration: Configuration): Option[File] = {
    val pidFilePath = configuration
      .getOptional[String]("play.server.pidfile.path")
      .getOrElse(throw ServerStartException("Pid file path not configured"))
    if (pidFilePath == "/dev/null") None
    else {
      val pidFile = new File(pidFilePath).getAbsoluteFile
      val pid     = process.pid.getOrElse(throw ServerStartException("Couldn't determine current process's pid"))
      val out =
        try Files.newOutputStream(pidFile.toPath, StandardOpenOption.CREATE_NEW)
        catch {
          case _: FileAlreadyExistsException =>
            throw ServerStartException(s"This application is already running (or delete ${pidFile.getPath} file).")
        }
      try out.write(pid.getBytes)
      finally out.close()
      Some(pidFile)
    }
  }
}
