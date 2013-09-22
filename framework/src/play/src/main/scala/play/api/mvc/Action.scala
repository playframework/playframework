package play.api.mvc

import play.api.libs.iteratee._
import play.api._
import scala.concurrent._
import scala.language.higherKinds

import play.core.Execution.Implicits.internalContext

/**
 * An Handler handles a request.
 */
trait Handler

/**
 * A handler that is able to tag requests
 */
trait RequestTaggingHandler extends Handler {
  def tagRequest(request: RequestHeader): RequestHeader
}

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

trait EssentialAction extends (RequestHeader => Iteratee[Array[Byte], SimpleResult]) with Handler {

  /**
   * Returns itself, for better support in the routes file.
   *
   * @return itself
   */
  def apply() = this

}

object EssentialAction {

  def apply(f: RequestHeader => Iteratee[Array[Byte], SimpleResult]): EssentialAction = new EssentialAction {
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
  def apply(request: Request[A]): Future[SimpleResult]

  def apply(rh: RequestHeader): Iteratee[Array[Byte], SimpleResult] = parser(rh).mapM {
    case Left(r) =>
      Play.logger.trace("Got direct result from the BodyParser: " + r)
      Future.successful(r)
    case Right(a) =>
      val request = Request(rh, a)
      Play.logger.trace("Invoking action with request: " + request)
      Play.maybeApplication.map { app =>
        play.utils.Threads.withContextClassLoader(app.classloader) {
          apply(request)
        }
      }.getOrElse(Future.successful(Results.InternalServerError))
  }(executionContext)

  /**
   * The execution context to run this action in
   *
   * @return The execution context to run the action in
   */
  def executionContext: ExecutionContext = play.api.libs.concurrent.Execution.defaultContext

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
 * @tparam A the body content type
 */
trait BodyParser[+A] extends Function1[RequestHeader, Iteratee[Array[Byte], Either[SimpleResult, A]]] {
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
   *   Iteratee.fold(0) { (state, chunk) => state + chunk.size } map(size => Right(size))
   * }
   * }}}
   */
  def apply[T](f: RequestHeader => Iteratee[Array[Byte], Either[SimpleResult, T]]): BodyParser[T] = {
    apply("(no name)")(f)
  }

  /**
   * Create a BodyParser
   *
   * Example:
   * {{{
   * val bodySize = BodyParser("Body size") { request =>
   *   Iteratee.fold(0) { (state, chunk) => state + chunk.size } map(size => Right(size))
   * }
   * }}}
   */
  def apply[T](debugName: String)(f: RequestHeader => Iteratee[Array[Byte], Either[SimpleResult, T]]): BodyParser[T] = new BodyParser[T] {
    def apply(rh: RequestHeader) = f(rh)
    override def toString = "BodyParser(" + debugName + ")"
  }

}

/**
 * Provides helpers for creating `Action` values.
 */
trait ActionBuilder[R[_]] {
  self =>

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
  final def apply[A](bodyParser: BodyParser[A])(block: R[A] => Result): Action[A] = async(bodyParser) { req: R[A] =>
    block(req) match {
      case simple: SimpleResult => Future.successful(simple)
      case async: AsyncResult => async.unflatten
    }
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
  final def apply(block: R[AnyContent] => Result): Action[AnyContent] = apply(BodyParsers.parse.anyContent)(block)

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
  final def apply(block: => Result): Action[AnyContent] = apply(_ => block)

  /**
   * Constructs an `Action` that returns a future of a result, with default content, and no request parameter.
   *
   * For example:
   * {{{
   * val hello = Action.async {
   *   WS.url("http://www.playframework.com").get().map { r =>
   *     if (r.status == 200) Ok("The website is up") else NotFound("The website is down")
   *   }
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  final def async(block: => Future[SimpleResult]): Action[AnyContent] = async(_ => block)

  /**
   * Constructs an `Action` that returns a future of a result, with default content.
   *
   * For example:
   * {{{
   * val hello = Action.async { request =>
   *   WS.url(request.getQueryString("url").get).get().map { r =>
   *     if (r.status == 200) Ok("The website is up") else NotFound("The website is down")
   *   }
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  final def async(block: R[AnyContent] => Future[SimpleResult]): Action[AnyContent] = async(BodyParsers.parse.anyContent)(block)

  /**
   * Constructs an `Action` that returns a future of a result, with default content.
   *
   * For example:
   * {{{
   * val hello = Action.async { request =>
   *   WS.url(request.getQueryString("url").get).get().map { r =>
   *     if (r.status == 200) Ok("The website is up") else NotFound("The website is down")
   *   }
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  final def async[A](bodyParser: BodyParser[A])(block: R[A] => Future[SimpleResult]): Action[A] = composeAction(new Action[A] {
    def parser = composeParser(bodyParser)
    def apply(request: Request[A]) = try {
      invokeBlock(request, block)
    } catch {
      // NotImplementedError is not caught by NonFatal, wrap it
      case e: NotImplementedError => throw new RuntimeException(e)
      // LinkageError is similarly harmless in Play Framework, since automatic reloading could easily trigger it
      case e: LinkageError => throw new RuntimeException(e)
    }
    override def executionContext = ActionBuilder.this.executionContext
  })

  /**
   * Invoke the block.  This is the main method that an ActionBuilder has to implement, at this stage it can wrap it in
   * any other actions, modify the request object or potentially use a different class to represent the request.
   *
   * @param request The request
   * @param block The block of code to invoke
   * @return A future of the result
   */
  protected def invokeBlock[A](request: Request[A], block: R[A] => Future[SimpleResult]): Future[SimpleResult]

  /**
   * Compose the parser.  This allows the action builder to potentially intercept requests before they are parsed.
   *
   * @param bodyParser The body parser to compose
   * @return The composed body parser
   */
  protected def composeParser[A](bodyParser: BodyParser[A]): BodyParser[A] = bodyParser

  /**
   * Compose the action with other actions.  This allows mixing in of various actions together.
   *
   * @param action The action to compose
   * @return The composed action
   */
  protected def composeAction[A](action: Action[A]): Action[A] = action

  /**
   * Get the execution context to run the request in.  Override this if you want a custom execution context
   *
   * @return The execution context
   */
  protected def executionContext: ExecutionContext = play.api.libs.concurrent.Execution.defaultContext
}

/**
 * Helper object to create `Action` values.
 */
object Action extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = block(request)
}
