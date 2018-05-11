/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.io._
import java.nio.file.{ FileAlreadyExistsException, Files, StandardOpenOption }

import akka.Done
import akka.actor.CoordinatedShutdown
import play.api._

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Used to start servers in 'prod' mode, the mode that is
 * used in production. The application is loaded and started
 * immediately.
 */
object ProdServerStart {

  /**
   * Start a prod mode server from the command line.
   */
  def main(args: Array[String]): Unit = {
    val process = new RealServerProcess(args)
    start(process)
  }

  /**
   * Starts a Play server and application for the given process. The settings
   * for the server are based on values passed on the command line and in
   * various system properties. Crash out by exiting the given process if there
   * are any problems.
   *
   * @param process The process (real or abstract) to use for starting the
   * server.
   */
  def start(process: ServerProcess): ReloadableServer = {
    start(process, true)
  }

  /**
   * Starts a Play server and application for the given process. The settings
   * for the server are based on values passed on the command line and in
   * various system properties. Crash out by exiting the given process if there
   * are any problems.
   *
   * @param process The process (real or abstract) to use for starting the
   * server.
   * @param exitJvmOnStop This method may be invoked from a test trying to
   *                      simulate Prod in which case the JVM should not be exited.
   */
  def start(process: ServerProcess, exitJvmOnStop: Boolean = false): ReloadableServer = {

    //
    // ProdServerStart uses two modes:
    //  1) environmentMode is used to control the actual behavior of the application, the server and
    //     anything started or handled from this class. It should always be `Prod`
    //  2) actualMode is used to identify the actual context this class was invoked from. The main
    //     purpose of this Mode is to tune the execution of the shutdown logic depending on whether
    //     we're simulating a Mode.Prod or actually running in Mode.Prod. For example, some unit
    //     tests assert the behavior of some code in Mode.Prod but are unit tests nonetheless,
    //     therefore they must not exit the JVM.
    //
    val environmentMode = Mode.Prod

    try {

      // Read settings
      val config: ServerConfig = readServerConfigSettings(process)

      // Create a PID file before we do any real work
      val pidFile = createPidFile(process, config.configuration)

      try {

        //
        // Tuning the behavior of coordinated-shutdown depends on several factors and
        // `environmentMode` is not always indicating actual Production environment, for
        // this reason we trust only `actualMode`
        //
        val initialSettings: Map[String, AnyRef] =
          if (exitJvmOnStop) {
            Map(
              "akka.coordinated-shutdown.exit-jvm" -> "on"
            )
          } else {
            Map.empty[String, AnyRef]
          }

        // Start the application
        val application: Application = {
          val environment = Environment(config.rootDir, process.classLoader, environmentMode)
          val context = ApplicationLoader.Context.create(environment, initialSettings)
          val loader = ApplicationLoader(context)
          loader.load(context)
        }
        Play.start(application)

        // Start the server
        val serverProvider: ServerProvider = ServerProvider.fromConfiguration(process.classLoader, config.configuration)
        val server = serverProvider.createServer(config, application)

        process.addShutdownHook {
          server.stop()
        }

        application.coordinatedShutdown.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "remove-pid-file"){
          () =>
            // Must delete the PID file after stopping the server not before...
            // In case of unclean shutdown or failure, leave the PID file there!
            pidFile.foreach(_.delete())
            assert(!pidFile.exists(_.exists), "PID file should not exist!")
            Future successful Done
        }

        server
      } catch {
        case NonFatal(e) =>
          // Clean up pidfile when the server fails to start
          pidFile.foreach(_.delete())
          throw e
      }
    } catch {
      case ServerStartException(message, cause) =>
        process.exit(message, cause)
      case NonFatal(e) =>
        process.exit("Oops, cannot start the server.", cause = Some(e))
    }
  }

  /**
   * Read the server config from the current process's command
   * line args and system properties.
   */
  def readServerConfigSettings(process: ServerProcess): ServerConfig = {
    val configuration: Configuration = {
      val rootDirArg: Option[File] = process.args.headOption.map(new File(_))
      val rootDirConfig = rootDirArg.fold(Map.empty[String, String])(dir => ServerConfig.rootDirConfig(dir))
      Configuration.load(process.classLoader, process.properties, rootDirConfig, true)
    }

    val rootDir: File = {
      val path = configuration.getOptional[String]("play.server.dir")
        .getOrElse(throw ServerStartException("No root server path supplied"))
      val file = new File(path)
      if (!(file.exists && file.isDirectory)) {
        throw ServerStartException(s"Bad root server path: $path")
      }
      file
    }

    def parsePort(portType: String): Option[Int] = {
      configuration.getOptional[String](s"play.server.${portType}.port").flatMap {
        case "disabled" => None
        case str =>
          val i = try Integer.parseInt(str) catch {
            case _: NumberFormatException => throw ServerStartException(s"Invalid ${portType.toUpperCase} port: $str")
          }
          Some(i)
      }
    }

    val httpPort = parsePort("http")
    val httpsPort = parsePort("https")
    if ((httpPort orElse httpsPort).isEmpty) throw ServerStartException("Must provide either an HTTP or HTTPS port")

    val address = configuration.getOptional[String]("play.server.http.address").getOrElse("0.0.0.0")

    ServerConfig(
      rootDir = rootDir,
      port = httpPort,
      sslPort = httpsPort,
      address = address,
      mode = Mode.Prod,
      properties = process.properties,
      configuration = configuration
    )
  }

  /**
   * Create a pid file for the current process.
   */
  def createPidFile(process: ServerProcess, configuration: Configuration): Option[File] = {
    val pidFilePath = configuration.getOptional[String]("play.server.pidfile.path")
      .getOrElse(throw ServerStartException("Pid file path not configured"))
    if (pidFilePath == "/dev/null") None else {
      val pidFile = new File(pidFilePath).getAbsoluteFile
      val pid = process.pid getOrElse (throw ServerStartException("Couldn't determine current process's pid"))
      val out = try Files.newOutputStream(pidFile.toPath, StandardOpenOption.CREATE_NEW) catch {
        case _: FileAlreadyExistsException =>
          throw ServerStartException(s"This application is already running (Or delete ${pidFile.getPath} file).")
      }
      try out.write(pid.getBytes) finally out.close()

      Some(pidFile)
    }
  }

}
