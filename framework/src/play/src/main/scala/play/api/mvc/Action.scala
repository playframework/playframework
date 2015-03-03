/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import play.api.libs.iteratee._
import play.api._
import play.core.routing.{ HandlerInvoker, HandlerInvokerFactory, HandlerDef }
import scala.concurrent._
import scala.language.higherKinds

/**
 * An Handler handles a request. Play understands several types of handlers,
 * for example `EssentialAction`s and `WebSocket`s.
 *
 * The `Handler` used to handle the request is controlled by `GlobalSetting`s's
 * `onRequestReceived` method. The default implementation of
 * `onRequestReceived` delegates to `onRouteRequest` which calls the default
 * `Router`.
 */
trait Handler

/**
 * A handler that is able to tag requests. Usually mixed in to other handlers.
 */
trait RequestTaggingHandler extends Handler {
  def tagRequest(request: RequestHeader): RequestHeader
}

/**
 * An `EssentialAction` underlies every `Action`. Given a `RequestHeader`, an
 * `EssentialAction` consumes the request body (an `Array[Byte]`) and returns
 * a `Result`.
 *
 * An `EssentialAction` is a `Handler`, which means it is one of the objects
 * that Play uses to handle requests.
 */
trait EssentialAction extends (RequestHeader => Iteratee[Array[Byte], Result]) with Handler {

  /**
   * Returns itself, for better support in the routes file.
   *
   * @return itself
   */
  def apply() = this

}

/**
 * Helper for creating `EssentialAction`s.
 */
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

  import Action._

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
  def apply(request: Request[A]): Future[Result]

  def apply(rh: RequestHeader): Iteratee[Array[Byte], Result] = parser(rh).mapM {
    case Left(r) =>
      logger.trace("Got direct result from the BodyParser: " + r)
      Future.successful(r)
    case Right(a) =>
      val request = Request(rh, a)
      logger.trace("Invoking action with request: " + request)
      Play.maybeApplication.map { app =>
        play.utils.Threads.withContextClassLoader(app.classloader) {
          apply(request)
        }
      }.getOrElse {
        apply(request)
      }
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
trait BodyParser[+A] extends Function1[RequestHeader, Iteratee[Array[Byte], Either[Result, A]]] {
  self =>

  /**
   * Uses the provided function to transform the BodyParser's computed result
   * when the request body has been parsed.
   *
   * @param f a function for transforming the computed result
   * @param ec The context to execute the supplied function with.
   *        The context is prepared on the calling thread.
   * @return the transformed body parser
   * @see [[play.api.libs.iteratee.Iteratee#map]]
   */
  def map[B](f: A => B)(implicit ec: ExecutionContext): BodyParser[B] = {
    // prepare execution context as body parser object may cross thread boundary
    implicit val pec = ec.prepare()
    new BodyParser[B] {
      def apply(request: RequestHeader) =
        self(request).map { _.right.map(f) }(pec)
      override def toString = self.toString
    }
  }

  /**
   * Like map but allows the map function to execute asynchronously.
   *
   * @param f the async function to map the result of the body parser
   * @param ec The context to execute the supplied function with.
   *        The context prepared on the calling thread.
   * @return the transformed body parser
   * @see [[map]]
   * @see [[play.api.libs.iteratee.Iteratee#mapM]]
   */
  def mapM[B](f: A => Future[B])(implicit ec: ExecutionContext): BodyParser[B] = {
    // prepare execution context as body parser object may cross thread boundary
    implicit val pec = ec.prepare()
    new BodyParser[B] {
      def apply(request: RequestHeader) = self(request).mapM {
        case Right(a) =>
          // safe to execute `Right.apply` in same thread
          f(a).map(Right.apply)(Execution.trampoline)
        case left =>
          Future.successful(left.asInstanceOf[Either[Result, B]])
      }(pec)
      override def toString = self.toString
    }
  }

  /**
   * Uses the provided function to transform the BodyParser’s computed result
   * into another BodyParser to continue with.
   *
   * On Done of the Iteratee produced by this BodyParser, the result is passed
   * to the provided function, and the resulting BodyParser is given the same
   * RequestHeader and the Iteratee produced is used to continue consuming
   * input.
   *
   * @param f the function to produce a new body parser from the result of this body parser
   * @param ec The context to execute the supplied function with.
   *        The context is prepared on the calling thread.
   * @return the transformed body parser
   * @see [[play.api.libs.iteratee.Iteratee#flatMap]]
   */
  def flatMap[B](f: A => BodyParser[B])(implicit ec: ExecutionContext): BodyParser[B] = {
    // prepare execution context as body parser object may cross thread boundary
    implicit val pec = ec.prepare()
    new BodyParser[B] {
      def apply(request: RequestHeader) = self(request).flatMap {
        case Left(e) => Done(Left(e))
        case Right(a) => f(a)(request)
      }(pec)
      override def toString = self.toString
    }
  }

  /**
   * Like flatMap but allows the flatMap function to execute asynchronously.
   *
   * @param f the async function to produce a new body parser from the result of this body parser
   * @param ec The context to execute the supplied function with.
   *        The context is prepared on the calling thread.
   * @return the transformed body parser
   * @see [[flatMap]]
   * @see [[play.api.libs.iteratee.Iteratee#flatMapM]]
   */
  def flatMapM[B](f: A => Future[BodyParser[B]])(implicit ec: ExecutionContext): BodyParser[B] = {
    // prepare execution context as body parser object may cross thread boundary
    implicit val pec = ec.prepare()
    new BodyParser[B] {
      def apply(request: RequestHeader) = self(request).flatMapM {
        case Right(a) =>
          f(a).map { _.apply(request) }(pec)
        case left =>
          Future.successful {
            Done[Array[Byte], Either[Result, B]](left.asInstanceOf[Either[Result, B]])
          }
      }(pec)
      override def toString = self.toString
    }
  }

  /**
   * Uses the provided function to validate the BodyParser's computed result
   * when the request body has been parsed.
   *
   * The provided function can produce either a direct result, which will short
   * circuit any further Action, or a value of type B.
   *
   * Example:
   * {{{
   *   def validateJson[A : Reads] = parse.json.validate(
   *     _.validate[A].asEither.left.map(e => BadRequest(JsError.toFlatJson(e)))
   *   )
   * }}}
   *
   * @param f the function to validate the computed result of this body parser
   * @param ec The context to execute the supplied function with.
   *        The context is prepared on the calling thread.
   * @return the transformed body parser
   */
  def validate[B](f: A => Either[Result, B])(implicit ec: ExecutionContext): BodyParser[B] = {
    // prepare execution context as body parser object may cross thread boundary
    implicit val pec = ec.prepare()
    new BodyParser[B] {
      def apply(request: RequestHeader) = self(request).flatMap {
        case Left(e) => Done(Left(e), Input.Empty)
        case Right(a) => Done(f(a), Input.Empty)
      }(pec)
      override def toString = self.toString
    }
  }

  /**
   * Like validate but allows the validate function to execute asynchronously.
   *
   * @param f the async function to validate the computed result of this body parser
   * @param ec The context to execute the supplied function with.
   *        The context is prepared on the calling thread.
   * @return the transformed body parser
   * @see [[validate]]
   */
  def validateM[B](f: A => Future[Either[Result, B]])(implicit ec: ExecutionContext): BodyParser[B] = {
    // prepare execution context as body parser object may cross thread boundary
    implicit val pec = ec.prepare()
    new BodyParser[B] {
      def apply(request: RequestHeader) = self(request).flatMapM {
        case Right(a) =>
          // safe to execute `Done.apply` in same thread
          f(a).map(Done.apply[Array[Byte], Either[Result, B]](_))(Execution.trampoline)
        case left =>
          Future.successful {
            Done[Array[Byte], Either[Result, B]](left.asInstanceOf[Either[Result, B]])
          }
      }(pec)
      override def toString = self.toString
    }
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
  def apply[T](f: RequestHeader => Iteratee[Array[Byte], Either[Result, T]]): BodyParser[T] = {
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
  def apply[T](debugName: String)(f: RequestHeader => Iteratee[Array[Byte], Either[Result, T]]): BodyParser[T] = new BodyParser[T] {
    def apply(rh: RequestHeader) = f(rh)
    override def toString = "BodyParser(" + debugName + ")"
  }

}

/**
 * A builder for generic Actions that generalizes over the type of requests.
 * An ActionFunction[R,P] may be chained onto an existing ActionBuilder[R] to produce a new ActionBuilder[P] using andThen.
 * The critical (abstract) function is invokeBlock.
 * Most users will want to use ActionBuilder instead.
 *
 * @tparam R the type of the request on which this is invoked (input)
 * @tparam P the parameter type which blocks executed by this builder take (output)
 */
trait ActionFunction[-R[_], +P[_]] {
  self =>

  /**
   * Invoke the block.  This is the main method that an ActionBuilder has to implement, at this stage it can wrap it in
   * any other actions, modify the request object or potentially use a different class to represent the request.
   *
   * @param request The request
   * @param block The block of code to invoke
   * @return A future of the result
   */
  def invokeBlock[A](request: R[A], block: P[A] => Future[Result]): Future[Result]

  /**
   * Get the execution context to run the request in.  Override this if you want a custom execution context
   *
   * @return The execution context
   */
  protected def executionContext: ExecutionContext = play.api.libs.concurrent.Execution.defaultContext

  /**
   * Compose this ActionFunction with another, with this one applied first.
   *
   * @param other ActionFunction with which to compose
   * @return The new ActionFunction
   */
  def andThen[Q[_]](other: ActionFunction[P, Q]): ActionFunction[R, Q] = new ActionFunction[R, Q] {
    def invokeBlock[A](request: R[A], block: Q[A] => Future[Result]) =
      self.invokeBlock[A](request, other.invokeBlock[A](_, block))
  }

  /**
   * Compose another ActionFunction with this one, with this one applied last.
   *
   * @param other ActionFunction with which to compose
   * @return The new ActionFunction
   */
  def compose[Q[_]](other: ActionFunction[Q, R]): ActionFunction[Q, P] =
    other.andThen(this)

  def compose(other: ActionBuilder[R]): ActionBuilder[P] =
    other.andThen(this)

}

/**
 * Provides helpers for creating `Action` values.
 */
trait ActionBuilder[+R[_]] extends ActionFunction[Request, R] {
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
    Future.successful(block(req))
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
  final def apply(block: R[AnyContent] => Result): Action[AnyContent] = apply(BodyParsers.parse.default)(block)

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
  final def async(block: => Future[Result]): Action[AnyContent] = async(_ => block)

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
  final def async(block: R[AnyContent] => Future[Result]): Action[AnyContent] = async(BodyParsers.parse.default)(block)

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
  final def async[A](bodyParser: BodyParser[A])(block: R[A] => Future[Result]): Action[A] = composeAction(new Action[A] {
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

  override def andThen[Q[_]](other: ActionFunction[R, Q]): ActionBuilder[Q] = new ActionBuilder[Q] {
    def invokeBlock[A](request: Request[A], block: Q[A] => Future[Result]) =
      self.invokeBlock[A](request, other.invokeBlock[A](_, block))
    override protected def composeParser[A](bodyParser: BodyParser[A]): BodyParser[A] = self.composeParser(bodyParser)
    override protected def composeAction[A](action: Action[A]): Action[A] = self.composeAction(action)
  }
}

/**
 * Helper object to create `Action` values.
 */
object Action extends ActionBuilder[Request] {
  private val logger = Logger(Action.getClass)

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request)
}

/* NOTE: the following are all example uses of ActionFunction, each subtly
 * different but useful in different ways. They may not all be necessary. */

/**
 * A simple kind of ActionFunction which, given a request (of type R), may
 * either immediately produce a Result (for example, an error), or call
 * its Action block with a parameter (of type P).
 * The critical (abstract) function is refine.
 */
trait ActionRefiner[-R[_], +P[_]] extends ActionFunction[R, P] {
  /**
   * Determine how to process a request.  This is the main method than an ActionRefiner has to implement.
   * It can decide to immediately intercept the request and return a Result (Left), or continue processing with a new parameter of type P (Right).
   *
   * @param request the input request
   * @return Either a result or a new parameter to pass to the Action block
   */
  protected def refine[A](request: R[A]): Future[Either[Result, P[A]]]

  final def invokeBlock[A](request: R[A], block: P[A] => Future[Result]) =
    refine(request).flatMap(_.fold(Future.successful _, block))(executionContext)
}

/**
 * A simple kind of ActionRefiner which, given a request (of type R),
 * unconditionally transforms it to a new parameter type (P) to be passed to
 * its Action block.  The critical (abstract) function is transform.
 */
trait ActionTransformer[-R[_], +P[_]] extends ActionRefiner[R, P] {
  /**
   * Augment or transform an existing request.  This is the main method that an ActionTransformer has to implement.
   *
   * @param request the input request
   * @return The new parameter to pass to the Action block
   */
  protected def transform[A](request: R[A]): Future[P[A]]

  final def refine[A](request: R[A]) =
    transform(request).map(Right(_))(executionContext)
}

/**
 * A simple kind of ActionRefiner which, given a request (of type R), may
 * either immediately produce a Result (for example, an error), or
 * continue its Action block with the same request.
 * The critical (abstract) function is filter.
 */
trait ActionFilter[R[_]] extends ActionRefiner[R, R] {
  /**
   * Determine whether to process a request.  This is the main method that an ActionFilter has to implement.
   * It can decide to immediately intercept the request and return a Result (Some), or continue processing (None).
   *
   * @param request the input request
   * @return An optional Result with which to abort the request
   */
  protected def filter[A](request: R[A]): Future[Option[Result]]

  final protected def refine[A](request: R[A]) =
    filter(request).map(_.toLeft(request))(executionContext)
}
