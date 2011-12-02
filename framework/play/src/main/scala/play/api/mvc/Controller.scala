package play.api.mvc

import play.api.http._

/**
 * Defines an `Action` method that generate default action values.
 *
 * This makes it possible to share a common `Action` definition for a whole controller.
 */
trait ControllerLike {

  /**
   * Constructs an `Action`.
   *
   * For example:
   * {{{
   * val echo = Action(anyContentParser) { request =>
   *   Ok("Got request [" + request + "]")
   * }
   * }}}
   *
   * @tparam A the type of the request body
   * @param bodyParser the `BodyParser` to use to parse the request body
   * @param block the action code
   * @return an action
   */
  def Action[A](bodyParser: BodyParser[A])(block: Request[A] => Result): Action[A]

}

/**
 * Defines utility methods to generate `Action` and `Results` types.
 *
 * For example:
 * {{{
 * object Application extends Controller {
 *
 *   def hello(name:String) = Action { request =>
 *     Ok("Hello " + name)
 *   }
 *
 * }
 * }}}
 */
trait Controller extends ControllerLike with Results with BodyParsers with HeaderNames with ContentTypes {

  /**
   * Constructs an `Action` with default content, and no request parameter.
   *
   * For example:
   * {{{
   * val hello = Action {
   *   Ok("Hello!")
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  final def Action(block: => Result): Action[AnyContent] = this.Action((ctx: Request[AnyContent]) => block)

  /**
   * Constructs an `Action` with default content.
   *
   * For example:
   * {{{
   * val echo = Action { request =>
   *   Ok("Got request [" + request + "]")
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  final def Action(block: Request[AnyContent] => Result): Action[AnyContent] = this.Action[AnyContent](BodyParsers.parse.anyContent)(block)

  /**
   * Constructs an `Action`.
   *
   * For example:
   * {{{
   * val echo = Action(anyContentParser) { request =>
   *   Ok("Got request [" + request + "]")
   * }
   * }}}
   *
   * @tparam A the type of the request body
   * @param bodyParser the `BodyParser` to use to parse the request body
   * @param block the action code
   * @return an action
   */
  def Action[A](bodyParser: BodyParser[A])(block: Request[A] => Result): Action[A] = play.api.mvc.Action[A](bodyParser)(block)

  /**
   * Provides an empty `Action` implementation: the result is a standard ‘Not implemented yet’ result page.
   *
   * For example:
   * {{{
   * def index(name:String) = TODO
   * }}}
   */
  val TODO = Action {
    NotImplemented[play.api.templates.Html](views.html.defaultpages.todo())
  }

  /**
   * Retrieves the session implicitly from the request.
   *
   * For example:
   * {{{
   * def index(name:String) = Action { implicit request =>
   *   val username = session("username")
   *   Ok("Hello " + username)
   * }
   * }}}
   */
  implicit def session(implicit request: RequestHeader) = request.session

  /**
   * Retrieve the flash scope implicitly from the request.
   *
   * For example:
   * {{{
   * def index(name:String) = Action { implicit request =>
   *   val message = flash("message")
   *   Ok("Got " + message)
   * }
   * }}}
   */
  implicit def flash(implicit request: RequestHeader) = request.flash

}

