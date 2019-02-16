/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

/**
 * Defines a `Call`, which describes an HTTP request and can be used to create links or fill redirect data.
 *
 * These values are usually generated by the reverse router.
 *
 * @param method the request HTTP method
 * @param url the request URL
 */
case class Call(method: String, url: String, fragment: String = null) extends play.mvc.Call {

  override def unique(): Call = copy(url = uniquify(url))

  override def withFragment(fragment: String): Call = copy(fragment = fragment)

  /**
   * Transform this call to an absolute URL.
   *
   * {{{
   * import play.api.mvc.{ Call, RequestHeader }
   *
   * implicit val req: RequestHeader = myRequest
   * val url: String = Call("GET", "/url").absoluteURL()
   * // == "http://\$host/url", or "https://\$host/url" if secure
   * }}}
   */
  def absoluteURL()(implicit request: RequestHeader): String =
    absoluteURL(request.secure)

  /**
   * Transform this call to an absolute URL.
   */
  def absoluteURL(secure: Boolean)(implicit request: RequestHeader): String =
    "http" + (if (secure) "s" else "") + "://" + request.host + this.url + this.appendFragment

  /**
   * Transform this call to an WebSocket URL.
   *
   * {{{
   * import play.api.mvc.{ Call, RequestHeader }
   *
   * implicit val req: RequestHeader = myRequest
   * val url: String = Call("GET", "/url").webSocketURL()
   * // == "ws://\$host/url", or "wss://\$host/url" if secure
   * }}}
   */
  def webSocketURL()(implicit request: RequestHeader): String =
    webSocketURL(request.secure)

  /**
   * Transform this call to an WebSocket URL.
   */
  def webSocketURL(secure: Boolean)(implicit request: RequestHeader): String =
    "ws" + (if (secure) "s" else "") + "://" + request.host + this.url

  /**
   * Transform this call to a URL relative to the current request's path.
   *
   * {{{
   * import play.api.mvc.{ Call, RequestHeader }
   *
   * implicit val req: RequestHeader = myRequest
   * // If current req.path == "/playframework"
   * val url: String = Call("GET", "/url").relative
   * // == "../url"
   * }}}
   */
  def relative(implicit request: RequestHeader): String = this.relativeTo(request.path)

}
