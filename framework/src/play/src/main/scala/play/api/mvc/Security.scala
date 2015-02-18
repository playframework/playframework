/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import play.api._
import play.api.mvc.Results._

import play.api.libs.iteratee._
import scala.concurrent.Future

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

  /**
   * An authenticated request
   *
   * @param user The user that made the request
   */
  class AuthenticatedRequest[A, U](val user: U, request: Request[A]) extends WrappedRequest[A](request)

  /**
   * An authenticated action builder.
   *
   * This can be used to create an action builder, like so:
   *
   * {{{
   * // in a Security trait
   * object Authenticated extends AuthenticatedBuilder(req => getUserFromRequest(req))
   *
   * // then in a controller
   * def index = Authenticated { implicit request =>
   *   Ok("Hello " + request.user)
   * }
   * }}}
   *
   * It can also be used from an action builder, for example:
   *
   * {{{
   * class AuthenticatedDbRequest[A](val user: User,
   *                                 val conn: Connection,
   *                                 request: Request[A]) extends WrappedRequest[A](request)
   *
   * object Authenticated extends ActionBuilder[AuthenticatedDbRequest] {
   *   def invokeBlock[A](request: Request[A], block: (AuthenticatedDbRequest[A]) => Future[Result]) = {
   *     AuthenticatedBuilder(req => getUserFromRequest(req)).authenticate(request, { authRequest: AuthenticatedRequest[A, User] =>
   *       DB.withConnection { conn =>
   *         block(new AuthenticatedDbRequest[A](authRequest.user, conn, request))
   *       }
   *     })
   *   }
   * }
   * }}}
   *
   * @param userinfo The function that looks up the user info.
   * @param onUnauthorized The function to get the result for when no authenticated user can be found.
   */
  class AuthenticatedBuilder[U](userinfo: RequestHeader => Option[U],
    onUnauthorized: RequestHeader => Result = _ => Unauthorized(views.html.defaultpages.unauthorized()))
      extends ActionBuilder[({ type R[A] = AuthenticatedRequest[A, U] })#R] {

    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) =
      authenticate(request, block)

    /**
     * Authenticate the given block.
     */
    def authenticate[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) = {
      userinfo(request).map { user =>
        block(new AuthenticatedRequest(user, request))
      } getOrElse {
        Future.successful(onUnauthorized(request))
      }
    }
  }

  /**
   * An authenticated action builder.
   *
   * This can be used to create an action builder, like so:
   *
   * {{{
   * // in a Security trait
   * object Authenticated extends AuthenticatedBuilder(req => getUserFromRequest(req))
   *
   * // then in a controller
   * def index = Authenticated { implicit request =>
   *   Ok("Hello " + request.user)
   * }
   * }}}
   *
   * It can also be used from an action builder, for example:
   *
   * {{{
   * class AuthenticatedDbRequest[A](val user: User,
   *                                 val conn: Connection,
   *                                 request: Request[A]) extends WrappedRequest[A](request)
   *
   * object Authenticated extends ActionBuilder[AuthenticatedDbRequest] {
   *   def invokeBlock[A](request: Request[A], block: (AuthenticatedDbRequest[A]) => Future[Result]) = {
   *     AuthenticatedBuilder(req => getUserFromRequest(req)).authenticate(request, { authRequest: AuthenticatedRequest[A, User] =>
   *       DB.withConnection { conn =>
   *         block(new AuthenticatedDbRequest[A](authRequest.user, conn, request))
   *       }
   *     })
   *   }
   * }
   * }}}
   */
  object AuthenticatedBuilder {

    /**
     * Create an authenticated builder
     *
     * @param userinfo The function that looks up the user info.
     * @param onUnauthorized The function to get the result for when no authenticated user can be found.
     */
    def apply[U](userinfo: RequestHeader => Option[U],
      onUnauthorized: RequestHeader => Result = _ => Unauthorized(views.html.defaultpages.unauthorized())): AuthenticatedBuilder[U] = new AuthenticatedBuilder(userinfo, onUnauthorized)

    /**
     * Simple authenticated action builder that looks up the username from the session
     */
    def apply(): AuthenticatedBuilder[String] = apply[String](req => req.session.get(username))
  }
}

