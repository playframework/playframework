package play.api.mvc

import play.api.libs.iteratee._

/**
 * An action is basically a (Request[A] => Result) function,
 * handling a request and generating a result to be sent to the client.
 *
 * {{{
 * val echo = Action { request =>
 *   Ok("Got request [" + request + "]")
 * }
 * }}}
 *
 * @tparam A Type of the request body.
 */
trait Action[A] extends (Request[A] => Result) {

  /**
   * Type of the request body.
   */
  type BODY_CONTENT = A

  /**
   * Body parser associated with this action.
   * @see BodyParser
   */
  def parser: BodyParser[A]

  /**
   * Invoke this action.
   *
   * @param request The incoming HTTP request.
   * @return The result to be sent to the client.
   */
  def apply(request: Request[A]): Result

  /**
   * Compose this action with another action.
   *
   * Example:
   * {{{
   *   val actionWithLogger = anyAction.compose { (request, originalAction) =>
   *     Logger.info("Invoking " + originalAction)
   *     val result = originalAction(request)
   *     Logger.info("Got result: " + result)
   *     result
   *   }
   * }}}
   */
  def compose(composer: (Request[A], Action[A]) => Result) = {
    val self = this
    new Action[A] {
      def parser = self.parser
      def apply(request: Request[A]) = composer(request, self)
    }
  }

  /**
   * Useless, only for better support in the routes file.
   *
   * @return Itself.
   */
  def apply() = this

}

/**
 * A body parser parse the HTTP request body content.
 *
 * @tparam T Body content type.
 */
trait BodyParser[+T] extends Function1[RequestHeader, Iteratee[Array[Byte], T]]

/**
 * Helper object to construct BodyParser values.
 */
object BodyParser {

  def apply[T](f: Function1[RequestHeader, Iteratee[Array[Byte], T]]) = new BodyParser[T] {
    def apply(rh: RequestHeader) = f(rh)
  }

}

/**
 * Default content type of any request body.
 */
case class AnyContent(urlFormEncoded: Map[String, Seq[String]])

/**
 * Helper object to create Action values.
 */
object Action {

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
  def apply[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A] = new Action[A] {
    def parser = bodyParser
    def apply(ctx: Request[A]) = block(ctx)
  }

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
  def apply(block: Request[AnyContent] => Result): Action[AnyContent] = {
    Action(play.api.data.RequestData.urlEncoded("UTF-8" /* should get charset from content type */ ), block)
  }

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
  def apply(block: => Result): Action[AnyContent] = {
    this.apply(_ => block)
  }

}
