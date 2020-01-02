/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import play.api._
import play.api.libs.streams.Accumulator
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.reflectiveCalls
import scala.util.Failure
import scala.util.Success

/**
 * Helpers to create secure actions.
 */
object Security {
  private val logger = Logger(getClass)

  /**
   * The default error response for an unauthorized request; used multiple places here
   */
  private val DefaultUnauthorized: RequestHeader => Result = implicit request =>
    Unauthorized(views.html.defaultpages.unauthorized())

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
      onUnauthorized: RequestHeader => Result
  )(action: A => EssentialAction): EssentialAction = {
    EssentialAction { request =>
      userinfo(request)
        .map { user =>
          action(user)(request)
        }
        .getOrElse {
          Accumulator.done(onUnauthorized(request))
        }
    }
  }

  def WithAuthentication[A](
      userinfo: RequestHeader => Option[A]
  )(action: A => EssentialAction): EssentialAction = {
    Authenticated(userinfo, DefaultUnauthorized)(action)
  }

  /**
   * An authenticated request
   *
   * @param user The user that made the request
   */
  class AuthenticatedRequest[+A, U](val user: U, request: Request[A]) extends WrappedRequest[A](request) {
    protected override def newWrapper[B](newRequest: Request[B]): AuthenticatedRequest[B, U] =
      new AuthenticatedRequest[B, U](user, newRequest)
  }

  /**
   * An authenticated action builder.
   *
   * This can be used to create an action builder, like so:
   *
   * {{{
   * class UserAuthenticatedBuilder (parser: BodyParser[AnyContent])(implicit ec: ExecutionContext)
   *   extends AuthenticatedBuilder[User]({ req: RequestHeader =>
   *   req.session.get("user").map(User)
   * }, parser) {
   *   @Inject()
   *   def this(parser: BodyParsers.Default)(implicit ec: ExecutionContext) = {
   *     this(parser: BodyParser[AnyContent])
   *   }
   * }
   * }}}
   *
   * You can then use the authenticated builder with other action builders, i.e. to use a
   * messagesApi with authentication, you can add:
   *
   * {{{
   *  class AuthMessagesRequest[A](val user: User,
   *                              messagesApi: MessagesApi,
   *                              request: Request[A])
   * extends MessagesRequest[A](request, messagesApi)
   *
   * class AuthenticatedActionBuilder(val parser: BodyParser[AnyContent],
   *                                  messagesApi: MessagesApi,
   *                                  builder: AuthenticatedBuilder[User])
   *                                 (implicit val executionContext: ExecutionContext)
   *     extends ActionBuilder[AuthMessagesRequest, AnyContent] {
   *   type ResultBlock[A] = (AuthMessagesRequest[A]) => Future[Result]
   *
   *   @Inject
   *   def this(parser: BodyParsers.Default,
   *            messagesApi: MessagesApi,
   *            builder: UserAuthenticatedBuilder)(implicit ec: ExecutionContext) = {
   *     this(parser: BodyParser[AnyContent], messagesApi, builder)
   *   }
   *
   *   def invokeBlock[A](request: Request[A], block: ResultBlock[A]): Future[Result] = {
   *     builder.authenticate(request, { authRequest: AuthenticatedRequest[A, User] =>
   *       block(new AuthMessagesRequest[A](authRequest.user, messagesApi, request))
   *     })
   *   }
   * }
   * }}}
   *
   * @param userinfo The function that looks up the user info.
   * @param onUnauthorized The function to get the result for when no authenticated user can be found.
   */
  class AuthenticatedBuilder[U](
      userinfo: RequestHeader => Option[U],
      defaultParser: BodyParser[AnyContent],
      onUnauthorized: RequestHeader => Result = implicit request => Unauthorized(views.html.defaultpages.unauthorized())
  )(implicit val executionContext: ExecutionContext)
      extends ActionBuilder[({ type R[A] = AuthenticatedRequest[A, U] })#R, AnyContent] {
    lazy val parser = defaultParser

    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) =
      authenticate(request, block)

    /**
     * Authenticate the given block.
     */
    def authenticate[A](request: Request[A], block: (AuthenticatedRequest[A, U]) => Future[Result]) = {
      userinfo(request)
        .map { user =>
          block(new AuthenticatedRequest(user, request))
        }
        .getOrElse {
          Future.successful(onUnauthorized(request))
        }
    }
  }

  object AuthenticatedBuilder {

    /**
     * Create an authenticated builder
     *
     * @param userinfo The function that looks up the user info.
     * @param onUnauthorized The function to get the result for when no authenticated user can be found.
     */
    def apply[U](
        userinfo: RequestHeader => Option[U],
        defaultParser: BodyParser[AnyContent],
        onUnauthorized: RequestHeader => Result = DefaultUnauthorized
    )(implicit ec: ExecutionContext): AuthenticatedBuilder[U] = {
      new AuthenticatedBuilder(userinfo, defaultParser, onUnauthorized)
    }
  }
}
