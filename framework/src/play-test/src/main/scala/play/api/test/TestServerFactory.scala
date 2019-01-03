/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import java.util.concurrent.locks.Lock

import scala.util.control.NonFatal

import akka.annotation.ApiMayChange

import play.api.{ Application, Configuration, Mode }
import play.core.server.{ AkkaHttpServer, ServerConfig, ServerEndpoint, ServerEndpoints, ServerProvider }
import play.core.server.SelfSignedSSLEngineProvider

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
    val sc = ServerConfig(
      port = Some(0),
      sslPort = Some(0),
      mode = Mode.Test,
      rootDir = app.path)
    sc.copy(configuration = sc.configuration ++ overrideServerConfiguration(app))
  }

  protected def overrideServerConfiguration(app: Application): Configuration = Configuration(
    "play.server.https.engineProvider" -> classOf[SelfSignedSSLEngineProvider].getName,
    "play.server.akka.http2.enabled" -> true)

  protected def serverProvider(app: Application): ServerProvider = AkkaHttpServer.provider

  protected def serverEndpoints(testServer: TestServer): ServerEndpoints = {
    val httpEndpoint: Option[ServerEndpoint] = testServer.runningHttpPort.map(_ =>
      ServerEndpointRecipe.AkkaHttp11Plaintext.createEndpointFromServer(testServer)
    )
    val httpsEndpoint: Option[ServerEndpoint] = testServer.runningHttpsPort.map(_ =>
      ServerEndpointRecipe.AkkaHttp20Encrypted.createEndpointFromServer(testServer)
    )
    ServerEndpoints(httpEndpoint.toSeq ++ httpsEndpoint.toSeq)
  }

}
