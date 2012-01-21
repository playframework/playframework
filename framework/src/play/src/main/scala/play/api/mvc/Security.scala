package play.api.mvc

import play.api._
import play.api.mvc.Results._

import play.api.libs.iteratee._

/** Helpers to create secure actions. */
trait Security[User] {

  /**
   * Get the User from the request
   */
  def getUser(request: RequestHeader): Option[User]

  /**
   * Default handler for unauthorized requests
   */
  def onUnauthorized(request: RequestHeader): Result =
    Unauthorized(views.html.defaultpages.unauthorized())

  /**
   * Default handler for forbidden requests
   */
  def onForbidden(request: RequestHeader): Result =
    Unauthorized(views.html.defaultpages.forbidden())

  /**
   * Wraps another action, allowing only authorized HTTP requests.
   *
   * The user is retrieved from the session cookie using getUser function.
   * If user passes authorization test, the action is invoked with the user as an argument.
   *
   * For example:
   * {{{
   * Authorized(_.isAdmin) { user =>
   *   Action { request =>
   *     Ok("Hello " + user.name)
   *   }
   * }
   * }}}
   *
   * @tparam A the type of the request body
   * @param authorized function used to authorize the user
   * @param onUnauthorized function used to generate alternative result if the user is not authenticated - the default is a simple 401 page
   * @param action the action to wrap
   */
  def Authorized[A](
    authorized: User => Boolean,
    onUnauthorized: RequestHeader => Result = this.onUnauthorized,
    onForbidden: RequestHeader => Result = this.onForbidden
    )(action: User => Action[A]): Action[(Action[A], A)] = {

    val authenticatedBodyParser = BodyParser { request =>
      getUser(request).map { user =>
        if (authorized(user)) {
          val innerAction = action(user)
          innerAction.parser(request).mapDone { body =>
            body.right.map(innerBody => (innerAction, innerBody))
          }
        } else {
          Done(Left(onForbidden(request)),
            Input.Empty.asInstanceOf[Input[Array[Byte]]])
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
   * The user is retrieved from the session cookie and passed to the action
   * function as an argument
   *
   * For example:
   * {{{
   * Authenticated() { user =>
   *   Action { request =>
   *     Ok("Hello " + user)
   *   }
   * }
   * }}}
   *
   * @tparam A the type of the request body
   * @param onUnauthorized function used to generate alternative result if the user is not authenticated - the default is a simple 401 page
   * @param action the action to wrap
   */
  def Authenticated[A](onUnauthorized: RequestHeader => Result = this.onUnauthorized)(action: User => Action[A]): Action[(Action[A], A)] =
    Authorized(_ => true, onUnauthorized)(action)

}

/** Simple specialization of Security trait, getting User as a String stored in the session */
object Security extends Security[String] {

  /** Key of the username attribute stored in session. */
  lazy val username: String = Play.maybeApplication map (_.configuration.getString("session.username")) flatMap (e => e) getOrElse ("username")

  /** Get username from the session */
  override def getUser(request: RequestHeader): Option[String] =
    request.session.get(username)

}

