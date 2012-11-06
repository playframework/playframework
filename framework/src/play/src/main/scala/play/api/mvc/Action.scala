package play.api.mvc

import play.api.libs.iteratee._
import play.api._

/**
 * An Handler handles a request.
 */
trait Handler

/**
 * Reference to an Handler.
 */
class HandlerRef[T](callValue: => T, handlerDef: play.core.Router.HandlerDef)(implicit handlerInvoker: play.core.Router.HandlerInvoker[T]) extends play.mvc.HandlerRef {

  /**
   * Retrieve a real handler behind this ref.
   */
  def handler: play.api.mvc.Handler = {
    handlerInvoker.call(callValue, handlerDef)
  }

  /**
   * String representation of this Handler.
   */
  lazy val sym = {
    handlerDef.controller + "." + handlerDef.method + "(" + handlerDef.parameterTypes.map(_.getName).mkString(", ") + ")"
  }

  override def toString = {
    "HandlerRef[" + sym + ")]"
  }

}

trait EssentialAction extends (RequestHeader => Iteratee[Array[Byte], Result]) with Handler {

  /**
   * Returns itself, for better support in the routes file.
   *
   * @return itself
   */
  def apply() = this

}

object EssentialAction {

  def apply(f: RequestHeader => Iteratee[Array[Byte], Result]): EssentialAction = new EssentialAction {
    def apply(rh: RequestHeader) = f(rh)
  }
}

/**
 * An action is essentially a (Request[A] => Result) function that
 * handles a request and generates a result to be sent to the client.
 *
 * For example,
 * {{{
 * val echo = Action { request =>
 *   Ok("Got request [" + request + "]")
 * }
 * }}}
 *
 * @tparam A the type of the request body
 */
trait Action[A] extends EssentialAction {

  /**
   * Type of the request body.
   */
  type BODY_CONTENT = A

  /**
   * Body parser associated with this action.
   *
   * @see BodyParser
   */
  def parser: BodyParser[A]

  /**
   * Invokes this action.
   *
   * @param request the incoming HTTP request
   * @return the result to be sent to the client
   */
  def apply(request: Request[A]): Result

  //TODO make sure you use the right execution context
  def apply(rh: RequestHeader): Iteratee[Array[Byte], Result] = parser(rh).map {
    case Left(r) =>
      Logger("play").trace("Got direct result from the BodyParser: " + r)
      r
    case Right(a) =>
      val request = Request(rh, a)
      Logger("play").trace("Invoking action with request: " + request)
      Play.maybeApplication.map { app =>
        // try {
        play.utils.Threads.withContextClassLoader(app.classloader) {
          apply(request)
        }
        // } catch { case e => app.handleError(rh, e) }
      }.getOrElse(Results.InternalServerError)
  }

  /**
   * Returns itself, for better support in the routes file.
   *
   * @return itself
   */
  override def apply(): Action[A] = this

  override def toString = {
    "Action(parser=" + parser + ")"
  }

}

/**
 * A body parser parses the HTTP request body content.
 *
 * @tparam T the body content type
 */
trait BodyParser[+A] extends Function1[RequestHeader, Iteratee[Array[Byte], Either[Result, A]]] {
  self =>

  /**
   * Transform this BodyParser[A] to a BodyParser[B]
   */
  def map[B](f: A => B): BodyParser[B] = new BodyParser[B] {
    def apply(request: RequestHeader) = self(request).map(_.right.map(f(_)))
    override def toString = self.toString
  }

  /**
   * Transform this BodyParser[A] to a BodyParser[B]
   */
  def flatMap[B](f: A => BodyParser[B]): BodyParser[B] = new BodyParser[B] {
    def apply(request: RequestHeader) = self(request).flatMap {
      case Left(e) => Done(Left(e), Input.Empty)
      case Right(a) => f(a)(request)
    }
    override def toString = self.toString
  }

}

/**
 * Helper object to construct `BodyParser` values.
 */
object BodyParser {

  /**
   * Create an anonymous BodyParser
   *
   * Example:
   * {{{
   * val bodySize = BodyParser { request =>
   *   Iteratee.fold(0) { (state, chunk) => state + chunk.size } mapDone(size => Right(size))
   * }
   * }}}
   */
  def apply[T](f: Function1[RequestHeader, Iteratee[Array[Byte], Either[Result, T]]]): BodyParser[T] = {
    apply("(no name)")(f)
  }

  /**
   * Create a BodyParser
   *
   * Example:
   * {{{
   * val bodySize = BodyParser("Body size") { request =>
   *   Iteratee.fold(0) { (state, chunk) => state + chunk.size } mapDone(size => Right(size))
   * }
   * }}}
   */
  def apply[T](debugName: String)(f: Function1[RequestHeader, Iteratee[Array[Byte], Either[Result, T]]]): BodyParser[T] = new BodyParser[T] {
    def apply(rh: RequestHeader) = f(rh)
    override def toString = "BodyParser(" + debugName + ")"
  }

}

/**
 * Provides helpers for creating `Action` values.
 */
trait ActionBuilder {

  /**
   * Constructs an `Action`.
   *
   * For example:
   * {{{
   * val echo = Action(parse.anyContent) { request =>
   *   Ok("Got request [" + request + "]")
   * }
   * }}}
   *
   * @tparam A the type of the request body
   * @param bodyParser the `BodyParser` to use to parse the request body
   * @param block the action code
   * @return an action
   */
  def apply[A](bodyParser: BodyParser[A])(block: Request[A] => Result): Action[A] = new Action[A] {
    def parser = bodyParser
    def apply(ctx: Request[A]) = block(ctx)
  }

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
  def apply(block: Request[AnyContent] => Result): Action[AnyContent] = apply(BodyParsers.parse.anyContent)(block)

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
  def apply(block: => Result): Action[AnyContent] = apply(_ => block)

}

/**
 * Helper object to create `Action` values.
 */
object Action extends ActionBuilder
