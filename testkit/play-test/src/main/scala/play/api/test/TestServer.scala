/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import scala.util.control.NonFatal

import akka.annotation.ApiMayChange
import play.api._
import play.api.inject.guice.GuiceApplicationBuilder
import play.core.server._

/**
 * A test web server.
 *
 * @param config The server configuration.
 * @param application The Application to load in this server.
 * @param serverProvider The type of server to use. If not provided, uses Play's default provider.
 */
case class TestServer(config: ServerConfig, application: Application, serverProvider: Option[ServerProvider]) {
  private var testServerProcess: TestServerProcess = _
  private[test] var server: Server                 = _

  private def getTestServerIfRunning: Server = {
    val s = server
    if (s == null) {
      throw new IllegalStateException("Test server not running")
    }
    s
  }

  /**
   * Starts this server.
   */
  def start(): Unit = {
    if (testServerProcess != null) {
      sys.error("Server already started!")
    }

    try {
      testServerProcess = new TestServerProcess
      val resolvedServerProvider: ServerProvider = serverProvider.getOrElse {
        ServerProvider.fromConfiguration(testServerProcess.classLoader, config.configuration)
      }
      Play.start(application)
      server = resolvedServerProvider.createServer(config, application)
      testServerProcess.addShutdownHook {
        val ts = server
        server = null // Clear field before stopping, in case an error occurs
        ts.stop()
      }
    } catch {
      case NonFatal(t) =>
        t.printStackTrace
        throw new RuntimeException(t)
    }
  }

  /**
   * Stops this server.
   */
  def stop(): Unit = {
    if (testServerProcess != null) {
      val p = testServerProcess
      testServerProcess = null // Clear field before shutting, in case an error occurs
      p.shutdown()
    }
  }

  /**
   * The address that the server is running on.
   */
  def runningAddress: String = getTestServerIfRunning.mainAddress.getAddress.getHostAddress

  /**
   * The HTTP port that the server is running on.
   */
  def runningHttpPort: Option[Int] = getTestServerIfRunning.httpPort

  /**
   * The HTTPS port that the server is running on.
   */
  def runningHttpsPort: Option[Int] = getTestServerIfRunning.httpsPort

  /**
   * True if the server is running either on HTTP or HTTPS port.
   */
  @ApiMayChange
  def isRunning: Boolean = runningHttpPort.nonEmpty || runningHttpsPort.nonEmpty
}

object TestServer {

  /**
   * A test web server.
   *
   * @param port HTTP port to bind on.
   * @param application The Application to load in this server.
   * @param sslPort HTTPS port to bind on.
   * @param serverProvider The type of server to use. If not provided, uses Play's default provider.
   */
  def apply(
      port: Int,
      application: Application = GuiceApplicationBuilder().build(),
      sslPort: Option[Int] = None,
      serverProvider: Option[ServerProvider] = None
  ) = new TestServer(
    ServerConfig(
      address = Helpers.testServerAddress,
      port = Some(port),
      sslPort = sslPort,
      mode = Mode.Test,
      rootDir = application.path
    ),
    application,
    serverProvider
  )
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
  override def args        = Seq()
  override def properties  = System.getProperties
  override def pid         = None

  override def exit(message: String, cause: Option[Throwable] = None, returnCode: Int = -1): Nothing = {
    throw new TestServerExitException(message, cause, returnCode)
  }
}

private[play] case class TestServerExitException(message: String, cause: Option[Throwable] = None, returnCode: Int = -1)
    extends Exception(s"Exit with $message, $cause, $returnCode", cause.orNull)
