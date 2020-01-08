/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import java.util.concurrent.locks.Lock

import akka.annotation.ApiMayChange
import play.api.Application
import play.api.Configuration
import play.api.Mode
import play.core.server._

import scala.util.control.NonFatal

/** Creates a server for an application. */
@ApiMayChange trait TestServerFactory {
  def start(app: Application): RunningServer
}

@ApiMayChange object DefaultTestServerFactory extends DefaultTestServerFactory

/**
 * Creates a server for an application with both HTTP and HTTPS ports
 * using a self-signed certificate.
 *
 * Most logic in this class is in a protected method so that users can
 * extend the class and override its logic.
 */
@ApiMayChange class DefaultTestServerFactory extends TestServerFactory {
  override def start(app: Application): RunningServer = {
    val testServer = new TestServer(serverConfig(app), app, Some(serverProvider(app)))

    val appLock: Option[Lock] = optionalGlobalLock(app)
    appLock.foreach(_.lock())

    val stopServer = new AutoCloseable {
      def close(): Unit = {
        testServer.stop()
        appLock.foreach(_.unlock())
      }
    }

    try {
      testServer.start()
      RunningServer(app, serverEndpoints(testServer), stopServer)
    } catch {
      case NonFatal(e) =>
        stopServer.close()
        throw e
    }
  }

  /**
   * Get the lock (if any) that should be used to prevent concurrent
   * applications from running.
   */
  protected def optionalGlobalLock(app: Application): Option[Lock] = {
    if (app.globalApplicationEnabled) Some(PlayRunners.mutex) else None
  }

  protected def serverConfig(app: Application) = {
    val sc = ServerConfig(port = Some(0), sslPort = Some(0), mode = Mode.Test, rootDir = app.path)
    sc.copy(configuration = overrideServerConfiguration(app).withFallback(sc.configuration))
  }

  protected def overrideServerConfiguration(app: Application): Configuration =
    Configuration("play.server.https.engineProvider" -> classOf[SelfSignedSSLEngineProvider].getName)

  protected def serverProvider(app: Application): ServerProvider =
    ServerProvider.fromConfiguration(getClass.getClassLoader, serverConfig(app).configuration)

  protected def serverEndpoints(testServer: TestServer): ServerEndpoints =
    if (testServer.isRunning) testServer.server.serverEndpoints else ServerEndpoints.empty
}
