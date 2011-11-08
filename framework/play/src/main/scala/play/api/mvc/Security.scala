package play.api.mvc

import play.api._
import play.api.mvc.Results._

/** Helpers to create secure actions. */
object Security {

  /** Key of the USERNAME attribute stored in session. */
  val USERNAME = "username"

  /** Wraps another action, allowing only authenticated HTTP requests.
    *
    * The user name is retrieved from the session cookie, and added to the HTTP request’s
    * `username` attribute.
    *
    * For example:
    * {{{
    * Authenticated {
    *   Action { request =>
    *     Ok(request.username.map("Hello " + _))
    *   }
    * }
    * }}}
    *
    * @tparam A the type of the request body
    * @param username function used to retrieve the user name from the request header - the default is to read from session cookie
    * @param onUnauthorized function used to generate alternative result if the user is not authenticated - the default is a simple 401 page
    * @param action the action to wrap
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

  /** Wraps another action, allowing only authenticated HTTP requests.
    *
    * The user name is retrieved from the session cookie, and added to the HTTP request’s
    * `username` attribute.
    *
    * For example:
    * {{{
    * Authenticated {
    *   Action { request =>
    *     Ok(request.username.map("Hello " + _))
    *   }
    * }
    * }}}
    *
    * @tparam A the type of the request body
    * @param action the action to wrap
    */
  def Authenticated[A](action: Action[A]): Action[A] = Authenticated()(action)

  /** Redefines the default controller action method, to make all actions authenticated.
    *
    * For example:
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
    self: Controller =>

    /** Overrides the default action method.
     *
     * All actions defined in this controller will be wrapped in an `Authenticated` action.
     *
     * @see Authenticated
     */
    override abstract def Action[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A] = {
      Authenticated(
        onUnauthorized = (req => onUnauthorized(req)),
        username = (req => username(req)))(super.Action(bodyParser, block))
    }

    /** Retrieves the user name from the request header - the default is to read from session cookie.
     *
     * @param request the request header
     * @return the user name
     */
    def username(request: RequestHeader): Option[String] = request.session.get("username")

    /** Generates an alternative result if the user is not authenticated - the default is a simple 401 page.
     *
     * @param request the request header
     * @return the result to be sent to the user if the request was not authenticated
     */
    def onUnauthorized(request: RequestHeader): Result = Unauthorized(views.html.defaultpages.unauthorized())

  }

}

