package play.api.mvc

import play.api._
import play.api.mvc.Results._

import play.api.libs.iteratee._

/**
 * Helpers to create secure actions.
 */
object Security {

  /**
   * Wraps another action, allowing only authenticated HTTP requests.
   * Furthermore, it lets users to configure where to retrieve the user info from
   * and what to do in case unsuccessful authentication
   *
   * For example:
   * {{{
   *  //in a Security trait
   *  def username(request: RequestHeader) = request.session.get("email")
   *  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)
   *  def isAuthenticated(f: => String => Request[AnyContent] => Result) = {
   *    Authenticated(username, onUnauthorized) { user =>
   *      Action(request => f(user)(request))
   *    }
   *  }
   * //then in a controller
   * def index = isAuthenticated { username => implicit request =>
   *     Ok("Hello " + username)
   * }
   * }}}
   *
   * @tparam A the type of the user info value (e.g. `String` if user info consists only in a user name)
   * @param userinfo function used to retrieve the user info from the request header
   * @param onUnauthorized function used to generate alternative result if the user is not authenticated
   * @param action the action to wrap
   */
  def Authenticated[A](
    userinfo: RequestHeader => Option[A],
    onUnauthorized: RequestHeader => Result)(action: A => EssentialAction): EssentialAction = {

    EssentialAction { request =>
      userinfo(request).map { user =>
        action(user)(request)
      }.getOrElse {
        Done(onUnauthorized(request), Input.Empty)
      }
    }

  }

  /**
   * Key of the username attribute stored in session.
   */
  lazy val username: String = Play.maybeApplication.flatMap(_.configuration.getString("session.username")) getOrElse ("username")

  /**
   * Wraps another action, allowing only authenticated HTTP requests.
   *
   * The user name is retrieved from the (configurable) session cookie, and added to the HTTP request’s
   * `username` attribute. In case of failure it returns an Unauthorized response (401)
   *
   * For example:
   * {{{
   *  //in a Security trait
   *  def isAuthenticated(f: => String => Request[AnyContent] => Result) = {
   *    Authenticated { user =>
   *      Action(request => f(user)(request))
   *    }
   *  }
   * //then in a controller
   * def index = isAuthenticated { username => implicit request =>
   *     Ok("Hello " + username)
   * }
   * }}}
   *
   * @param action the action to wrap
   */
  def Authenticated(action: String => EssentialAction): EssentialAction = Authenticated(
    req => req.session.get(username),
    _ => Unauthorized(views.html.defaultpages.unauthorized()))(action)

}

