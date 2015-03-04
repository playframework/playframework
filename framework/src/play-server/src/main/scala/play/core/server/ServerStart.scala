/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import java.io._
import java.util.Properties
import play.api.{ Configuration, Mode }
import play.core._
import play.utils.Threads
import scala.util.control.NonFatal

/**
 * Helper for starting a Play server and application. The `main` method
 * is the entry point to a Play server running in production mode. The
 * `mainDev*` methods are used to start a server running in development
 * mode.
 */
trait ServerStart {

  /**
   * The ServerProvider to use if not overridden by a system property.
   */
  protected def defaultServerProvider: ServerProvider

  /**
   * Creates an ApplicationProvider for prod mode. Needed so we can mock
   * out the ApplicationProvider for testing.
   */
  protected def createApplicationProvider(config: ServerConfig): ApplicationProvider = {
    new StaticApplication(config.rootDir)
  }

  /**
   * Start a prod mode server from the command line. Calls `start`.
   */
  def main(args: Array[String]) {
    val process = new RealServerProcess(args)
    start(process)
  }

  /**
   * Starts a Play server and application for the given process. The settings
   * for the server are based on values passed on the command line and in
   * various system properties. Crash out by exiting the given process if there
   * are any problems.
   * @param process The process (real or abstract) to use for starting the
   * server.
   */
  def start(process: ServerProcess): ServerWithStop = {
    try {
      // Read settings
      val config = readServerConfigSettings(process)
      val serverProvider = readServerProviderSetting(process, config.configuration).getOrElse(defaultServerProvider)
      // Get the party started!
      val pidFile = createPidFile(process, config.configuration)
      val appProvider = createApplicationProvider(config)
      val server = serverProvider.createServer(config, appProvider)
      process.addShutdownHook {
        server.stop()
        pidFile.foreach(_.delete())
        assert(!pidFile.exists(_.exists), "PID file should not exist!")
      }
      server
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
      process.args.headOption match {
        case None =>
          ServerConfig.loadConfiguration(process.classLoader, process.properties)
        case Some(rootDir) =>
          // rootDir will become play.server.dir setting
          ServerConfig.loadConfiguration(process.classLoader, process.properties, new File(rootDir))
      }
    }

    val rootDir: File = {
      val path = configuration.getString("play.server.dir").getOrElse(throw ServerStartException("No root server path supplied"))
      val file = new File(path)
      if (!(file.exists && file.isDirectory)) {
        throw ServerStartException(s"Bad root server path: $path")
      }
      file
    }

    val httpPort = configuration.getString("play.server.http.port").flatMap {
      case "disabled" => None
      case str =>
        val i = try Integer.parseInt(str) catch {
          case _: NumberFormatException => throw ServerStartException(s"Invalid HTTP port: $str")
        }
        Some(i)
    }
    val httpsPort = configuration.getInt("play.server.https.port")
    if (!(httpPort orElse httpsPort).isDefined) throw ServerStartException("Must provide either an HTTP or HTTPS port")

    val address = configuration.getString("play.server.http.address").getOrElse("0.0.0.0")

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
   * Read the ServerProvider setting from the given process's
   * configuration. If not configured, returns None. If you need
   * a ServerProvider you can may want to use the
   * `defaultServerProvider` when this method returns None.
   */
  def readServerProviderSetting(process: ServerProcess, configuration: Configuration): Option[ServerProvider] = {
    configuration.getString("play.server.provider").map { className =>
      val clazz = try process.classLoader.loadClass(className) catch {
        case _: ClassNotFoundException => throw ServerStartException(s"Couldn't find ServerProvider class '$className'")
      }
      if (!classOf[ServerProvider].isAssignableFrom(clazz)) throw ServerStartException(s"Class ${clazz.getName} must implement ServerProvider interface")
      val ctor = try clazz.getConstructor() catch {
        case _: NoSuchMethodException => throw ServerStartException(s"ServerProvider class ${clazz.getName} must have a public default constructor")
      }
      ctor.newInstance().asInstanceOf[ServerProvider]
    }
  }

  /**
   * Create a pid file for the current process, and register a hook
   * to delete the file on process termination.
   */
  def createPidFile(process: ServerProcess, configuration: Configuration): Option[File] = {
    val pidFilePath = configuration.getString("play.server.pidfile.path").getOrElse(throw ServerStartException("Pid file path not configured"))
    if (pidFilePath == "/dev/null") None else {
      val pidFile = new File(pidFilePath).getAbsoluteFile

      if (pidFile.exists) {
        throw ServerStartException(s"This application is already running (Or delete ${pidFile.getPath} file).")
      }

      val pid = process.pid getOrElse (throw ServerStartException("Couldn't determine current process's pid"))
      val out = new FileOutputStream(pidFile)
      try out.write(pid.getBytes) finally out.close()

      Some(pidFile)
    }
  }

  /**
   * Provides an HTTPS-only server for the dev environment.
   *
   * <p>This method uses simple Java types so that it can be used with reflection by code
   * compiled with different versions of Scala.
   */
  def mainDevOnlyHttpsMode(
    buildLink: BuildLink,
    buildDocHandler: BuildDocHandler,
    httpsPort: Int,
    httpAddress: String): ServerWithStop = {
    mainDev(buildLink, buildDocHandler, None, Some(httpsPort), httpAddress)
  }

  /**
   * Provides an HTTP server for the dev environment
   *
   * <p>This method uses simple Java types so that it can be used with reflection by code
   * compiled with different versions of Scala.
   */
  def mainDevHttpMode(
    buildLink: BuildLink,
    buildDocHandler: BuildDocHandler,
    httpPort: Int,
    httpAddress: String): ServerWithStop = {
    mainDev(buildLink, buildDocHandler, Some(httpPort), Option(System.getProperty("https.port")).map(Integer.parseInt(_)), httpAddress)
  }

  private def mainDev(
    buildLink: BuildLink,
    buildDocHandler: BuildDocHandler,
    httpPort: Option[Int],
    httpsPort: Option[Int],
    httpAddress: String): ServerWithStop = {
    Threads.withContextClassLoader(this.getClass.getClassLoader) {
      try {
        val process = new RealServerProcess(args = Seq.empty)
        val config = ServerConfig(
          rootDir = buildLink.projectPath,
          port = httpPort,
          sslPort = httpsPort,
          address = httpAddress,
          mode = Mode.Dev,
          properties = process.properties
        )
        val appProvider = new ReloadableApplication(buildLink, buildDocHandler)
        val serverProvider = readServerProviderSetting(process, config.configuration).getOrElse(defaultServerProvider)
        serverProvider.createServer(config, appProvider)
      } catch {
        case e: ExceptionInInitializerError => throw e.getCause
      }

    }
  }

}

final case class ServerStartException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)
