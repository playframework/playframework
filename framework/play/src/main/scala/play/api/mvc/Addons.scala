package play.api.mvc

import play.api._
import play.api.mvc.Results._

object Security {

  val USERNAME = "username"

  def Authenticated[A](
    username: RequestHeader => Option[String] = (req => req.session.get(USERNAME)),
    onUnauthorized: RequestHeader => Result = (_ => Unauthorized(html.views.defaultpages.unauthorized())))(action: Action[A]): Action[A] = {

    action.compose { (request, originalAction) =>
      username(request).map { authenticatedUser =>
        val authenticatedRequest = new Request[A] {
          def uri = request.uri
          def path = request.path
          def method = request.method
          def queryString = request.queryString
          def headers = request.headers
          def cookies = request.cookies
          def username = Some(authenticatedUser)
          val body = request.body
        }
        originalAction(authenticatedRequest)
      }.getOrElse {
        onUnauthorized(request)
      }
    }

  }

  def Authenticated[A](action: Action[A]): Action[A] = Authenticated()(action)

  trait AllAuthenticated extends ControllerLike {
    self: Controller =>

    override abstract def Action[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A] = {
      Authenticated(
        onUnauthorized = (req => onUnauthorized(req)),
        username = (req => username(req)))(super.Action(bodyParser, block))
    }

    def username(request: RequestHeader): Option[String] = request.session.get("username")
    def onUnauthorized(request: RequestHeader): Result = Unauthorized(html.views.defaultpages.unauthorized())

  }

}

