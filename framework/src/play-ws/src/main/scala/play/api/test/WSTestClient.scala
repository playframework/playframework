package play.api.test

import play.api.libs.ws._

import play.api.mvc.Call

trait WsTestClient {

  type Port = Int

  /**
   * Constructs a WS request for the given reverse route.
   *
   * For example:
   * {{{
   *   wsCall(controllers.routes.Application.index()).get()
   * }}}
   */
  def wsCall(call: Call)(implicit port: Port, client: WSClient = WS.client(play.api.Play.current)): WSRequestHolder = wsUrl(call.url)

  /**
   * Constructs a WS request holder for the given relative URL.
   */
  def wsUrl(url: String)(implicit port: Port, client: WSClient = WS.client(play.api.Play.current)) = {
    WS.clientUrl("http://localhost:" + port + url)
  }

}
