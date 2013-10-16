package play.api.test

import play.api.libs.ws.WS

import play.api.mvc.Call

trait WsTestClient {

  type Port = Int

  /**
   * Construct a WS request for the given reverse route.
   *
   * For example:
   * {{{
   *   wsCall(controllers.routes.Application.index()).get()
   * }}}
   */
  def wsCall(call: Call)(implicit port: Port): WS.WSRequestHolder = wsUrl(call.url)

  /**
   * Construct a WS request for the given relative URL.
   */
  def wsUrl(url: String)(implicit port: Port): WS.WSRequestHolder = WS.url("http://localhost:" + port + url)
}
