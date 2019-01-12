/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import java.io.Closeable
import java.util.concurrent.TimeUnit

import akka.actor.{ ActorSystem, Terminated }
import akka.stream.ActorMaterializer
import com.typesafe.sslconfig.ssl.{ SSLConfigSettings, SSLLooseConfig }
import org.specs2.execute.AsResult
import org.specs2.specification.core.Fragment
import play.api.Configuration
import play.api.libs.ws.ahc.{ AhcWSClient, AhcWSClientConfig }
import play.api.libs.ws.{ WSClient, WSClientConfig, WSRequest, WSResponse }
import play.api.mvc.Call
import play.api.test.{ ApplicationFactory, DefaultAwaitTimeout, FutureAwaits }
import play.core.server.ServerEndpoint

import scala.annotation.implicitNotFound
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

/**
 * Provides a similar interface to [[play.api.test.WsTestClient]], but
 * connects to an integration test's [[ServerEndpoint]] instead of an
 * arbitrary scheme and port.
 */
trait WSEndpointSupport {
  self: EndpointIntegrationSpecification with FutureAwaits with DefaultAwaitTimeout =>

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
      client.url(s"${endpoint.scheme}://localhost:" + endpoint.port + path)
    }
    /**
     * Make a request to the endpoint using the given path.
     */
    def makeRequest(path: String): WSResponse = {
      await(buildRequest(path).get())
    }
  }

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
   * Takes a [[ServerEndpoint]], creates a matching [[WSEndpoint]], calls
   * a block of code on the client and then closes the client afterwards.
   *
   * Most users should use [[WSApplicationFactory.withAllWSEndpoints()]]
   * instead of this method.
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

  /**
   * Implicit class that enhances [[ApplicationFactory]] with the [[withAllWSEndpoints()]] method.
   */
  implicit class WSApplicationFactory(appFactory: ApplicationFactory) {
    /**
     * Helper that creates a specs2 fragment for the server endpoints given in
     * [[allEndpointRecipes]]. Each fragment creates an application, starts a server,
     * starts a [[WSClient]] and runs the given block of code.
     *
     * {{{
     * withResult(Results.Ok("Hello")) withAllWSEndpoints {
     *   wsEndpoint: WSEndpoint =>
     *     val response = wsEndpoint.makeRequest("/")
     *     response.body must_== "Hello"
     * }
     * }}}
     */
    def withAllWSEndpoints[A: AsResult](block: WSEndpoint => A): Fragment =
      appFactory.withAllEndpoints { endpoint: ServerEndpoint => withWSEndpoint(endpoint)(block) }
  }
}
