package play.api.mvc

import play.api._
import play.api.mvc.Results._

import play.api.libs.iteratee._

/**
 * Helpers to create secure actions.
 */
object Security {

  /**
   * Key of the username attribute stored in session.
   */
  lazy val username: String = Play.maybeApplication map (_.configuration.getString("session.username")) flatMap (e => e) getOrElse ("username")

  /**
   * Wraps another action, allowing only authenticated HTTP requests.
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
   * Wraps another action, allowing only authenticated HTTP requests.
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
  def Authenticated[A](action: String => Action[A]): Action[(Action[A], A)] = Authenticated(
    req => req.session.get(username),
    _ => Unauthorized(views.html.defaultpages.unauthorized()))(action)

}

