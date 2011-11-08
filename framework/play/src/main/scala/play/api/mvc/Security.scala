package play.api.mvc

import play.api._
import play.api.mvc.Results._

/**
 * Helpers to create secure actions
 */
object Security {

  /**
   * Key of the USERNAME attribute stored in session.
   */
  val USERNAME = "username"

  /**
   * Wrap another action, allowing only authenticated HTTP requests.
   *
   * The username is retrieved from the session cookie, and added to the HTTP request
   * username attribute.
   *
   * Example:
   * {{{
   * Authenticated {
   *   Action { request =>
   *     Ok(request.username.map("Hello " + _))
   *   }
   * }
   * }}}
   *
   * @tparam A Type of the request body.
   * @param username Function used to retrieve the username from the request header (default is to read from session cookie).
   * @param onUnauthorized Function used to generate alternative result if the user is not authenticated (default to simple 401 page)
   * @param action Action to wrap.
   */
  def Authenticated[A](
    username: RequestHeader => Option[String] = (req => req.session.get(USERNAME)),
    onUnauthorized: RequestHeader => Result = (_ => Unauthorized(views.html.defaultpages.unauthorized())))(action: Action[A]): Action[A] = {

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

  /**
   * Wrap another action, allowing only authenticated HTTP requests.
   *
   * The username is retrieved from the session cookie, and added to the HTTP request
   * username attribute.
   *
   * Example:
   * {{{
   * Authenticated {
   *   Action { request =>
   *     Ok(request.username.map("Hello " + _))
   *   }
   * }
   * }}}
   *
   * @tparam A Type of the request body.
   * @param action Action to wrap.
   */
  def Authenticated[A](action: Action[A]): Action[A] = Authenticated()(action)

  /**
   * Redefines the default Controller Action method, to make all actions authenticated.
   *
   * Example:
   * {{{
   * object Admin extends Controller with AllAuthenticated {
   *
   *   def index = Action { request =>
   *     Ok(request.username.map("Hello " + _))
   *   }
   *
   * }
   * }}}
   */
  trait AllAuthenticated extends ControllerLike {

    /**
     * Override the default Action method.
     *
     * All Action defined in this controller will be wrapped in an Authenticated action.
     *
     * @see Authenticated
     */
    override abstract def Action[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A] = {
      Authenticated(
        onUnauthorized = (req => onUnauthorized(req)),
        username = (req => username(req)))(super.Action(bodyParser, block))
    }

    /**
     * Retrieve the username from the request header (default is to read from session cookie).
     *
     * @param request The request header.
     * @return Maybe the username.
     */
    def username(request: RequestHeader): Option[String] = request.session.get("username")

    /**
     * Generate alternative result if the user is not authenticated (default to simple 401 page)
     *
     * @param request The request header.
     * @return The result to be sent to the user if the request was not authenticated.
     */
    def onUnauthorized(request: RequestHeader): Result = Unauthorized(views.html.defaultpages.unauthorized())

  }

}

