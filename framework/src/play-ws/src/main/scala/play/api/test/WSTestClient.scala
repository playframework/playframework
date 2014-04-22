package play.api.test

import play.api.libs.ws._

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
  def wsCall(call: Call)(implicit port: Port, client: WSClient = WS.client(play.api.Play.current)): WSRequestHolder = wsUrl(call.url)

  /**
   * Constructs a WS request holder for the given relative URL.  Optionally takes a port and WSClient.  Note that the WS client used
   * by default requires a running Play application (use WithApplication for tests).
   */
  def wsUrl(url: String)(implicit port: Port, client: WSClient = WS.client(play.api.Play.current)) = {
    WS.clientUrl("http://localhost:" + port + url)
  }

}
