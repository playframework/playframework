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
   * Furthermore, it lets users to configure where to retrieve the username from
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
   * @tparam A the type of the request body
   * @param username function used to retrieve the user name from the request header - the default is to read from session cookie
   * @param onUnauthorized function used to generate alternative result if the user is not authenticated - the default is a simple 401 page
   * @param action the action to wrap
   */
  def Authenticated[A](
    username: RequestHeader => Option[String],
    onUnauthorized: RequestHeader => Result)(action: String => Action[A]): Action[(Action[A], A)] = {

    val authenticatedBodyParser = BodyParser { request =>
      username(request).map { user =>
        val innerAction = action(user)
        innerAction.parser(request).mapDone { body =>
          body.right.map(innerBody => (innerAction, innerBody))
        }
      }.getOrElse {
        Done(Left(onUnauthorized(request)), Input.Empty)
      }
    }

    Action(authenticatedBodyParser) { request =>
      val (innerAction, innerBody) = request.body
      innerAction(request.map(_ => innerBody))
    }

  }

  /**
   * Key of the username attribute stored in session.
   */
  lazy val username: String = Play.maybeApplication map (_.configuration.getString("session.username")) flatMap (e => e) getOrElse ("username")

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
   * @tparam A the type of the request body
   * @param action the action to wrap
   */
  def Authenticated[A](action: String => Action[A]): Action[(Action[A], A)] = Authenticated(
    req => req.session.get(username),
    _ => Unauthorized(views.html.defaultpages.unauthorized()))(action)

}

