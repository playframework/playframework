/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws._
import play.api.libs.ws.ahc.{ AhcWSClient, AhcWSClientConfig }
import play.api.mvc.Call

/**
 * A standalone test client that is useful for running standalone integration tests.
 */
trait WsTestClient {

  type Port = Int

  private val clientProducer: (Port, String) => WSClient = { (port, scheme) =>
    new WsTestClient.InternalWSClient(scheme, port)
  }

  /**
   * Constructs a WS request for the given reverse route.  Optionally takes a WSClient producing function.  Note that the WS client used
   * by default requires a running Play application (use WithApplication for tests).
   *
   * For example:
   * {{{
   * "work" in new WithApplication() { implicit app =>
   *   wsCall(controllers.routes.Application.index()).get()
   * }
   * }}}
   */
  def wsCall(call: Call)(implicit port: Port, client: (Port, String) => WSClient = clientProducer, scheme: String = "http"): WSRequest = {
    wsUrl(call.url)
  }

  /**
   * Constructs a WS request holder for the given relative URL.  Optionally takes a scheme, a port, or a client producing function.  Note that the WS client used
   * by default requires a running Play application (use WithApplication for tests).
   */
  def wsUrl(url: String)(implicit port: Port, client: (Port, String) => WSClient = clientProducer, scheme: String = "http"): WSRequest = {
    client(port, scheme).url(s"$scheme://localhost:" + port + url)
  }

  /**
   * Run the given block of code with a client.
   *
   * The client passed to the block of code supports absolute path relative URLs passed to the url method.  If an
   * absolute path relative URL is used, the protocol is assumed to be http, the host localhost, and the port is the
   * implicit port parameter passed to this method.  This is designed to work smoothly with the Server.with* methods,
   * for example:
   *
   * {{{
   * Server.withRouter() {
   *   case GET(p"/hello/\$who") => Action(Ok("Hello " + who))
   * } { implicit port =>
   *   withClient { ws =>
   *     await(ws.url("/hello/world").get()).body must_== "Hello world"
   *   }
   * }
   * }}}
   *
   * @param block The block of code to run
   * @param port The port
   * @return The result of the block of code
   */
  def withClient[T](block: WSClient => T)(implicit port: play.api.http.Port = new play.api.http.Port(-1), scheme: String = "http") = {
    val client = clientProducer(port.value, scheme)
    try {
      block(client)
    } finally {
      client.close()
    }
  }
}

object WsTestClient extends WsTestClient {

  // Accept the overhead of having a client open through all the integration tests
  private lazy val singletonInstance: WSClient = {
    val name = "ws-test-client"
    val system = ActorSystem(name)
    val materializer = ActorMaterializer(namePrefix = Some(name))(system)
    // Don't retry for tests
    AhcWSClient(AhcWSClientConfig(maxRequestRetry = 0))(materializer)
  }

  /**
   * Creates a standalone WSClient, using its own ActorSystem and Netty thread pool.
   *
   * This client has no dependencies at all on the underlying system, but is wasteful of resources.
   *
   * @param port the port to connect to the server on.
   * @param scheme the scheme to connect on ("http" or "https")
   */
  class InternalWSClient(scheme: String, port: Port) extends WSClient {

    def underlying[T] = singletonInstance.underlying.asInstanceOf[T]

    def url(url: String): WSRequest = {
      if (url.startsWith("/") && port != -1) {
        val httpPort = new play.api.http.Port(port)
        singletonInstance.url(s"$scheme://localhost:$httpPort$url")
      } else {
        singletonInstance.url(url)
      }
    }

    /** Closes this client, and releases underlying resources. */
    override def close(): Unit = {}
  }
}
