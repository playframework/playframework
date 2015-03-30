package play.api.test

import play.api.libs.ws._
import play.api.libs.ws.ning.{ NingWSClientConfig, NingWSClient }

import play.api.mvc.Call

trait WsTestClient {

  type Port = Int

  /**
   * Constructs a WS request holder for the given reverse route.  Optionally takes a WSClient.  Note that the WS client used
   * by default requires a running Play application (use WithApplication for tests).
   *
   * For example:
   * {{{
   * "work" in new WithApplication() { implicit app =>
   *   wsCall(controllers.routes.Application.index()).get()
   * }
   * }}}
   */
  def wsCall(call: Call)(implicit port: Port, client: WSClient = WS.client(play.api.Play.current)): WSRequest = wsUrl(call.url)

  /**
   * Constructs a WS request holder for the given relative URL.  Optionally takes a port and WSClient.  Note that the WS client used
   * by default requires a running Play application (use WithApplication for tests).
   */
  def wsUrl(url: String)(implicit port: Port, client: WSClient = WS.client(play.api.Play.current)) = {
    WS.clientUrl("http://localhost:" + port + url)
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
   *   case GET(p"/hello/$who") => Action(Ok("Hello " + who))
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
  def withClient[T](block: WSClient => T)(implicit port: play.api.http.Port = new play.api.http.Port(-1)) = {
    // Don't retry for tests
    val client = NingWSClient(NingWSClientConfig(maxRequestRetry = 0))
    val wrappedClient = new WSClient {
      def underlying[T] = client.underlying.asInstanceOf[T]
      def url(url: String) = {
        if (url.startsWith("/") && port.value != -1) {
          client.url(s"http://localhost:$port$url")
        } else {
          client.url(url)
        }
      }
      def close() = ()
    }

    try {
      block(wrappedClient)
    } finally {
      client.close()
    }
  }

}

object WsTestClient extends WsTestClient
