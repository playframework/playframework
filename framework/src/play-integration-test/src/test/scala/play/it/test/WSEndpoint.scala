/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import java.io.Closeable
import java.util.concurrent.TimeUnit

import akka.actor.{ ActorSystem, Terminated }
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.sslconfig.ssl.{ SSLConfigSettings, SSLLooseConfig }
import play.api.Configuration
import play.api.libs.ws.ahc.{ AhcWSClient, AhcWSClientConfig }
import play.api.libs.ws.{ WSClient, WSClientConfig, WSRequest, WSResponse }
import play.api.mvc.Call
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }

import scala.annotation.implicitNotFound
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

/** Describes a [[WSClient]] that is bound to a particular [[ServerEndpoint]]. */
@implicitNotFound("Use withAllWSEndpoints { implicit wsEndpoint: WSEndpoint => ... } to get a value")
trait WSEndpoint {
  /** The endpoint to connect to. */
  def endpoint: ServerEndpoint
  /** The client to connect with. */
  def client: WSClient
  /**
   * Build a request to the endpoint using the given path.
   */
  def buildRequest(path: String): WSRequest = {
    client.url(endpoint.pathUrl(path))
  }
  /**
   * Make a request to the endpoint using the given path.
   */
  def makeRequest(path: String)(implicit timeout: Timeout): WSResponse = {
    Await.result(buildRequest(path).get(), timeout.duration)
  }
}

object WSEndpoint {
  /**
   * Takes a [[ServerEndpoint]], creates a matching [[WSEndpoint]], calls
   * a block of code on the client and then closes the client afterwards.
   *
   * Most users should use `useWS` instead of this method.
   */
  def withWSEndpoint[A](endpoint: ServerEndpoint)(block: WSEndpoint => A): A = {
    val e = endpoint // Avoid a name clash

    val serverClient = new WSEndpoint with Closeable {
      override val endpoint = e
      private val actorSystem: ActorSystem = {
        val actorConfig = Configuration(
          "akka.loglevel" -> "WARNING"
        )
        ActorSystem("WSEndpointSupport", actorConfig.underlying)
      }
      override val client: WSClient = {
        // Set up custom config to trust any SSL certificate. Unfortunately
        // even though we have the certificate information already loaded
        // we can't easily get it to our WSClient due to limitations in
        // the ssl-config library.
        val sslLooseConfig: SSLLooseConfig = SSLLooseConfig().withAcceptAnyCertificate(true)
        val sslConfig: SSLConfigSettings = SSLConfigSettings().withLoose(sslLooseConfig)
        val wsClientConfig: WSClientConfig = WSClientConfig(ssl = sslConfig)
        val ahcWsClientConfig = AhcWSClientConfig(wsClientConfig = wsClientConfig, maxRequestRetry = 0)

        implicit val materializer = ActorMaterializer(namePrefix = Some("WSEndpointSupport"))(actorSystem)
        AhcWSClient(ahcWsClientConfig)
      }
      override def close(): Unit = {
        client.close()
        val terminated: Future[Terminated] = actorSystem.terminate()
        Await.ready(terminated, Duration(20, TimeUnit.SECONDS))
      }
    }
    try block(serverClient) finally serverClient.close()
  }
}

/**
 * Provides a similar interface to [[play.api.test.WsTestClient]], but
 * connects to an integration test's [[ServerEndpoint]] instead of an
 * arbitrary scheme and port.
 */
trait WSEndpointSupport {
  self: EndpointIntegrationSpecification with FutureAwaits with DefaultAwaitTimeout =>

  import WSEndpoint._

  /**
   * Build a request to the running server endpoint at the given path.
   *
   * This method is provided as a drop-in replacement for the methods in
   * the [[play.api.test.WsTestClient]] class. However, you should use
   * the methods on the [[WSEndpoint]] object directly, if possible.
   */
  @deprecated("Use WSEndpoint.buildRequest or .makeRequest instead", "2.6.4")
  def wsUrl(path: String)(implicit endpointClient: WSEndpoint): WSRequest = {
    endpointClient.buildRequest(path)
  }
  /**
   * Build a request to the running server endpoint using the given call.
   *
   * This method is provided as a drop-in replacement for the methods in
   * the [[play.api.test.WsTestClient]] class. However, you should use
   * the methods on the [[WSEndpoint]] object directly, if possible.
   */
  @deprecated("Use WSEndpoint.buildRequest(call.url) or .makeRequest(call.url) instead", "2.6.4")
  def wsCall(call: Call)(implicit endpointClient: WSEndpoint): WSRequest = {
    endpointClient.buildRequest(call.url)
  }

  /**
   * Get the client used to connect to the running server endpoint.
   *
   * This method is provided as a drop-in replacement for the methods in
   * the [[play.api.test.WsTestClient]] class. However, you should use
   * the methods on the [[WSEndpoint]] object directly, if possible.
   */
  @deprecated("Use WSEndpoint.client, .buildRequest() or .makeRequest() instead", "2.6.4")
  def withClient[T](block: WSClient => T)(implicit endpointClient: WSEndpoint): T = block(endpointClient.client)

  /**
   * Implicit class that enhances [[ApplicationFactory]] with methods to connect with a WSClient.
   */
  implicit final class WSApplicationFactoryOps(appFactory: ApplicationFactory) {
    def useWS: WSEndpointOps = new WSEndpointOps(new PlainServerEndpointOps(appFactory))
  }

  /**
   * Provides a [[WSEndpoint]] for each underlying endpoint.
   *
   * Also provides the `request` method to conveniently make a request.
   */
  final class WSEndpointOps(delegate: ServerEndpointOps[ServerEndpoint]) extends AdaptingServerEndpointOps[WSEndpoint, ServerEndpoint](delegate) {
    override protected def adaptBlock[A](block: WSEndpoint => A): ServerEndpoint => A = withWSEndpoint(_)(block)
    override protected def withDelegate(newDelegate: ServerEndpointOps[ServerEndpoint]): WSEndpointOps =
      new WSEndpointOps(newDelegate)
    def request(path: String): WSResponseEndpointOps = new WSResponseEndpointOps(path, this)
  }

  /**
   * Makes a request and provides the `WSResponse` for each underlying endpoint.
   */
  final class WSResponseEndpointOps(path: String, delegate: ServerEndpointOps[WSEndpoint]) extends AdaptingServerEndpointOps[WSResponse, WSEndpoint](delegate) {
    override protected def adaptBlock[A](block: WSResponse => A): WSEndpoint => A = { wsEndpoint: WSEndpoint =>
      val response: WSResponse = wsEndpoint.makeRequest(path)
      block(response)
    }
    override protected def withDelegate(newDelegate: ServerEndpointOps[WSEndpoint]): WSResponseEndpointOps =
      new WSResponseEndpointOps(path, newDelegate)
  }

}
