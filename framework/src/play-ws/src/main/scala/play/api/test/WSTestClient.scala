package play.api.test

import play.api.libs.ws._

import play.api.mvc.Call

import play.api.Play.current

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
  def wsCall(call: Call)(implicit port: Port): WSRequestHolder = wsUrl(call.url)

  /**
   * Construct a WS request for the given relative URL.
   */
  def wsUrl(url: String)(implicit port: Port): WSRequestHolder = WS.url("http://localhost:" + port + url)
}
