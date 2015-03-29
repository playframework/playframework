/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.test

import play.api._
import play.core.server._
import scala.util.control.NonFatal

/**
 * A test web server.
 *
 * @param port HTTP port to bind on.
 * @param application The Application to load in this server.
 * @param sslPort HTTPS port to bind on.
 * @param serverProvider *Experimental API; subject to change* The type of
 * server to use. If not provided, uses Play's default provider.
 */
case class TestServer(
    port: Int,
    application: Application = FakeApplication(),
    sslPort: Option[Int] = None,
    serverProvider: Option[ServerProvider] = None) {

  private var testServerProcess: TestServerProcess = _

  /**
   * Starts this server.
   */
  def start() {
    if (testServerProcess != null) {
      sys.error("Server already started!")
    }

    try {
      val config = ServerConfig(
        rootDir = application.path,
        port = Option(port), sslPort = sslPort, mode = Mode.Test,
        properties = System.getProperties
      )
      testServerProcess = TestServer.start(serverProvider, config, application)
    } catch {
      case NonFatal(t) =>
        t.printStackTrace
        throw new RuntimeException(t)
    }
  }

  /**
   * Stops this server.
   */
  def stop() {
    if (testServerProcess != null) {
      val shuttingDownProcess = testServerProcess
      testServerProcess = null
      shuttingDownProcess.shutdown()
    }
  }

}

object TestServer {

  /**
   * Start a TestServer with the given config and application. To stop it,
   * call `shutdown` on the returned TestServerProcess.
   */
  private[play] def start(
    testServerProvider: Option[ServerProvider],
    config: ServerConfig,
    application: Application): TestServerProcess = {
    val process = new TestServerProcess
    val serverStart: ServerStart = new ServerStart {
      def defaultServerProvider = ??? // Won't be called because we're not using any ServerStart methods to create the server.
    }
    val serverProvider: ServerProvider = {
      testServerProvider
    } orElse {
      serverStart.readServerProviderSetting(process, config.configuration)
    } getOrElse NettyServer.defaultServerProvider
    val appProvider = new play.core.TestApplication(application)
    val server = serverProvider.createServer(config, appProvider)
    process.addShutdownHook { server.stop() }
    process
  }

}

/**
 * A mock system process for a TestServer to run within. A ServerProcess
 * can mock command line arguments, System properties, a ClassLoader,
 * System.exit calls and shutdown hooks.
 *
 * When the process is finished, call `shutdown()` to run all registered
 * shutdown hooks.
 */
private[play] class TestServerProcess extends ServerProcess {

  private var hooks = Seq.empty[() => Unit]
  override def addShutdownHook(hook: => Unit) = {
    hooks = hooks :+ (() => hook)
  }
  def shutdown(): Unit = {
    for (h <- hooks) h.apply()
  }

  override def classLoader = getClass.getClassLoader
  override def args = Seq()
  override def properties = System.getProperties
  override def pid = None

  override def exit(message: String, cause: Option[Throwable] = None, returnCode: Int = -1): Nothing = {
    throw new TestServerExitException(message, cause, returnCode)
  }

}

private[play] case class TestServerExitException(
  message: String,
  cause: Option[Throwable] = None,
  returnCode: Int = -1) extends Exception(s"Exit with $message, $cause, $returnCode", cause.orNull)
