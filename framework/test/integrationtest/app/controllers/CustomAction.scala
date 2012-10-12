package controllers

import play.api.mvc._

/**
 *
 * @author blake
 *
 */
object CustomAction extends ActionBuilder[CustomRequest] {
  def request[A] = (rh: RequestHeader, a:A) => CustomRequest("XXX",Request(rh,a))
}

case class CustomRequest[A](token: String, request: Request[A]) extends WrappedRequest(request)


