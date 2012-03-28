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
   *
   * The default key is `username`, but it can be changed through the `session.username` configuration setting.
   */
  lazy val username: String = Play.maybeApplication map (_.configuration.getString("session.username")) flatMap (e => e) getOrElse ("username")

  /**
   * Wraps an action, allowing only authenticated HTTP requests.
   *
   *
   * @tparam A the type of the request body
   * @param username a function used to retrieve the user name from the request header
   * @param onUnauthorized a function used to generate an alternative result if the user is not authenticated
   * @param actionForIdentifier a function that returns the Action to execute if the user is authenticated. Takes the username as parameter.
   */
  def Authenticated[A](
    username: RequestHeader => Option[String],
    onUnauthorized: RequestHeader => Result)(actionForIdentifier: String => Action[A]): Action[(Action[A], A)] = {

    val authenticatedBodyParser = BodyParser { request =>
      username(request).map { user =>
        val innerAction = actionForIdentifier(user)
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
   * Wraps an action, allowing only authenticated HTTP requests.
   *
   * The user name is retrieved from the session cookie, and added to the HTTP request’s
   * `username` attribute. If authentication fails, a simple 401 page is displayed.
   *
   * For example:
   * {{{
   * Authenticated {
   *   username: String => Action {
   *     Ok(("Hello " + username))
   *   }
   * }
   * }}}
   *
   * @tparam A the type of the request body
   * @param a function which returns the Action to be executed if the user is authenticated.
   *        It takes the username extracted from the request as parameter.
   */
  def Authenticated[A](actionForIdentifier: String => Action[A]): Action[(Action[A], A)] = Authenticated(
    req => req.session.get(username),
    _ => Unauthorized(views.html.defaultpages.unauthorized()))(actionForIdentifier)

}

