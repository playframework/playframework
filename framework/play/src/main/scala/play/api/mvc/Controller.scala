package play.api.mvc

/**
 * Defines an Action method that generate default action values.
 * It allows to share a common Action definition for a whole controller.
 */
trait ControllerLike {

  /**
   * Construct an Action.
   *
   * Example:
   * {{{
   * val echo = Action(anyContentParser) { request =>
   *   Ok("Got request [" + request + "]")
   * }
   * }}}
   *
   * @tparam A Type of the request body.
   * @param bodyParser The BodyParser to use to parse the request body.
   * @param block The action code.
   * @return An action.
   */
  def Action[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A]

}

/**
 * Defines utility methods to generate Action and Results types.
 *
 * Example:
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
trait Controller extends ControllerLike with Results with play.api.http.HeaderNames {

  /**
   * Construct an Action with default content, and no request parameter.
   *
   * Example:
   * {{{
   * val hello = Action {
   *   Ok("Hello!")
   * }
   * }}}
   *
   * @param block The action code.
   * @return An action.
   */
  final def Action(block: => Result): Action[AnyContent] = this.Action((ctx: Request[AnyContent]) => block)

  /**
   * Construct an Action with default content.
   *
   * Example:
   * {{{
   * val echo = Action { request =>
   *   Ok("Got request [" + request + "]")
   * }
   * }}}
   *
   * @param block The action code.
   * @return An action.
   */
  final def Action(block: Request[AnyContent] => Result): Action[AnyContent] = this.Action[AnyContent](play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */ ), block)

  /**
   * Construct an Action.
   *
   * Example:
   * {{{
   * val echo = Action(anyContentParser) { request =>
   *   Ok("Got request [" + request + "]")
   * }
   * }}}
   *
   * @tparam A Type of the request body.
   * @param bodyParser The BodyParser to use to parse the request body.
   * @param block The action code.
   * @return An action.
   */
  def Action[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A] = play.api.mvc.Action[A](bodyParser, block)

  /**
   * Provide empty implementation of Action. The result is a standard 'Not implemented yet' result page.
   *
   * Example:
   * {{{
   * def index(name:String) = TODO
   * }}}
   */
  val TODO = Action {
    NotImplemented(views.html.defaultpages.todo())
  }

  /**
   * Retrieve the session implicitly from the request.
   *
   * Example:
   * {{{
   * def index(name:String) = Action { implicit request =>
   *   val username = session("username")
   *   Ok("Hello " + username)
   * }
   * }}}
   */
  implicit def session(implicit request: RequestHeader) = request.session

}

