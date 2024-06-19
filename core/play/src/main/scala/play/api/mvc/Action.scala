/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import javax.inject.Inject

import scala.concurrent._

import org.apache.pekko.util.ByteString
import play.api._
import play.api.libs.streams.Accumulator
import play.api.mvc.request.RequestAttrKey
import play.core.Execution
import play.utils.ExecCtxUtils

/**
 * An `EssentialAction` underlies every `Action`. Given a `RequestHeader`, an
 * `EssentialAction` consumes the request body (an `ByteString`) and returns
 * a `Result`.
 *
 * An `EssentialAction` is a `Handler`, which means it is one of the objects
 * that Play uses to handle requests.
 */
trait EssentialAction extends (RequestHeader => Accumulator[ByteString, Result]) with Handler { self =>

  /** @return itself, for better support in the routes file. */
  def apply(): EssentialAction = this

  def asJava: play.mvc.EssentialAction = new play.mvc.EssentialAction() {
    def apply(rh: play.mvc.Http.RequestHeader) = self(rh.asScala).map(_.asJava)(Execution.trampoline).asJava
    override def apply(rh: RequestHeader)      = self(rh)
  }
}

/**
 * Helper for creating `EssentialAction`s.
 */
object EssentialAction {
  def apply(f: RequestHeader => Accumulator[ByteString, Result]): EssentialAction = f(_)
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
  private lazy val logger = Logger(getClass)

  /** Type of the request body. */
  type BODY_CONTENT = A

  /** Body parser associated with this action. */
  def parser: BodyParser[A]

  /**
   * Invokes this action.
   *
   * @param request the incoming HTTP request
   * @return the result to be sent to the client
   */
  def apply(request: Request[A]): Future[Result]

  def apply(rh: RequestHeader): Accumulator[ByteString, Result] =
    if (rh.attrs.contains(RequestAttrKey.DeferredBodyParsing)) {
      val request = Request(rh, null.asInstanceOf[A]) // not parsing, therefore no body
      logger.trace("Deferring body parsing for request: " + rh)
      // We skip parsing but immediately run the action
      Accumulator.done(apply(request))
    } else {
      logger.trace("Not deferring body parsing for request, will parse now: " + rh)
      // Run the parser first and then call the action
      BodyParser.runParserThenInvokeAction(parser, rh, apply)(executionContext)
    }

  /** @return The execution context to run the action in */
  def executionContext: ExecutionContext

  /** @return itself, for better support in the routes file. */
  override def apply(): Action[A] = this

  override def toString = s"Action(parser=$parser)"
}

/**
 * A body parser parses the HTTP request body content.
 *
 * @tparam A the body content type
 */
trait BodyParser[+A] extends (RequestHeader => Accumulator[ByteString, Either[Result, A]]) {
  // "with Any" because we need to prevent 2.12 SAM inference here
  self: BodyParser[A] with Any =>

  /**
   * Uses the provided function to transform the BodyParser's computed result
   * when the request body has been parsed.
   *
   * @param f a function for transforming the computed result
   * @param ec The context to execute the supplied function with.
   *        The context is prepared on the calling thread.
   * @return the transformed body parser
   * @see play.api.libs.streams.Accumulator.map
   */
  def map[B](f: A => B)(implicit ec: ExecutionContext): BodyParser[B] = {
    // prepare execution context as body parser object may cross thread boundary
    implicit val pec = ExecCtxUtils.prepare(ec)
    new BodyParser[B] {
      def apply(request: RequestHeader) = self(request).map(_.map(f))(pec)
      override def toString             = self.toString
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
   * @see play.api.libs.streams.Accumulator.mapFuture[B]
   */
  def mapM[B](f: A => Future[B])(implicit ec: ExecutionContext): BodyParser[B] = {
    // prepare execution context as body parser object may cross thread boundary
    implicit val pec = ExecCtxUtils.prepare(ec)
    new BodyParser[B] {
      def apply(request: RequestHeader) =
        self(request).mapFuture {
          case Right(a) => f(a).map(Right.apply)(Execution.trampoline) // safe to execute `Right.apply` in same thread
          case left     => Future.successful(left.asInstanceOf[Either[Result, B]])
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
   *     _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
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
    implicit val pec = ExecCtxUtils.prepare(ec)
    new BodyParser[B] {
      def apply(request: RequestHeader) = self(request).map(_.flatMap(f))(pec)
      override def toString             = self.toString
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
    implicit val pec = ExecCtxUtils.prepare(ec)
    new BodyParser[B] {
      def apply(request: RequestHeader) =
        self(request).mapFuture {
          case Right(a) => f(a) // safe to execute `Done.apply` in same thread
          case Left(e)  => Future.successful(Left(e))
        }(pec)
      override def toString = self.toString
    }
  }
}

/**
 * Helper object to construct `BodyParser` values.
 */
object BodyParser {
  private lazy val logger = Logger(getClass)

  def apply[T](f: RequestHeader => Accumulator[ByteString, Either[Result, T]]): BodyParser[T] = apply("(no name)")(f)

  def apply[T](debugName: String)(f: RequestHeader => Accumulator[ByteString, Either[Result, T]]): BodyParser[T] =
    new BodyParser[T] {
      def apply(rh: RequestHeader) = f(rh)
      override def toString        = s"BodyParser($debugName)"
    }

  /**
   * When body parsing was deferred, this method can be used to eventually parse the body for a given request.
   * If this method was called already for a request (meaning the body was finally parsed already),
   * it won't do anything anymore and just pass the request through.
   *
   * @param parser the body parser to use to parse the requests' body
   * @param request the request whose body was not parsed yet (because parsing was explicitly deferred)
   * @param next called after the body was parsed
   * @param ec The context to execute the supplied next action with.
   *        The context is prepared on the calling thread.
   * @return the result to be sent to the client
   */
  def parseBody[A](parser: BodyParser[A], request: Request[A], next: Request[A] => Future[Result])(
      implicit ec: ExecutionContext
  ): Future[Result] = {
    request.attrs
      .get(RequestAttrKey.DeferredBodyParsing)
      .map(invokeAction => {
        logger.trace("Body parsing was deferred, eventually parsing now for request: " + request)
        invokeAction(
          // First runs the parser, then invokes the given "next" action
          // (We remove the request attribute in case calling this method multiple times it won't parse again)
          Future(
            runParserThenInvokeAction(parser, request.removeAttr(RequestAttrKey.DeferredBodyParsing), next)
          ),
          false
        )
      })
      .getOrElse {
        logger.trace("Not parsing body, it's a WebSocket or it was parsed before already for request: " + request)
        next(request)
      }
  }

  /**
   *  First runs the parser, then invokes the given "next" action. For internal use only.
   */
  private[play] def runParserThenInvokeAction[A](
      parser: BodyParser[A],
      rh: RequestHeader,
      next: Request[A] => Future[Result]
  )(implicit executionContext: ExecutionContext): Accumulator[ByteString, Result] =
    parser(rh).mapFuture {
      case Left(r) =>
        logger.trace("Got direct result from the BodyParser: " + r)
        Future.successful(r)
      case Right(a) =>
        val request = Request(rh, a)
        logger.trace("Invoking action with request: " + request)
        next(request)
    }(ExecCtxUtils.prepare(executionContext))
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

  /** @return The execution context to run the request in. */
  protected def executionContext: ExecutionContext

  /**
   * Compose this ActionFunction with another, with this one applied first.
   *
   * @param other ActionFunction with which to compose
   * @return The new ActionFunction
   */
  def andThen[Q[_]](other: ActionFunction[P, Q]): ActionFunction[R, Q] = new ActionFunction[R, Q] {
    def executionContext = self.executionContext
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

  def compose[B](other: ActionBuilder[R, B]): ActionBuilder[P, B] =
    other.andThen(this)
}

/**
 * Provides helpers for creating [[Action]] values.
 */
trait ActionBuilder[+R[_], B] extends ActionFunction[Request, R] {
  self =>

  /**
   * @return The BodyParser to be used by this ActionBuilder if no other is specified
   */
  def parser: BodyParser[B]

  /**
   * Constructs an [[ActionBuilder]] with the given [[BodyParser]]. The result can then be applied directly to a block.
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
   * @return an action
   */
  final def apply[A](bodyParser: BodyParser[A]): ActionBuilder[R, A] = new ActionBuilder[R, A] {
    override def parser                                                               = bodyParser
    protected override def executionContext                                           = self.executionContext
    protected override def composeParser[T](bodyParser: BodyParser[T]): BodyParser[T] = self.composeParser(bodyParser)
    protected override def composeAction[T](action: Action[T]): Action[T]             = self.composeAction(action)
    override def invokeBlock[T](request: Request[T], block: R[T] => Future[Result])   = self.invokeBlock(request, block)
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
  final def apply(block: R[B] => Result): Action[B] = async(block.andThen(Future.successful))

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
  final def apply(block: => Result): Action[AnyContent] =
    apply(BodyParsers.utils.ignore(AnyContentAsEmpty: AnyContent))(_ => block)

  /**
   * Constructs an `Action` that returns a future of a result, with default content, and no request parameter.
   *
   * For example:
   * {{{
   * val hello = Action.async {
   *   ws.url("http://www.playframework.com").get().map { r =>
   *     if (r.status == 200) Ok("The website is up") else NotFound("The website is down")
   *   }
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  final def async(block: => Future[Result]): Action[AnyContent] =
    async(BodyParsers.utils.ignore(AnyContentAsEmpty: AnyContent))(_ => block)

  /**
   * Constructs an `Action` that returns a future of a result, with default content.
   *
   * For example:
   * {{{
   * val hello = Action.async { request =>
   *   ws.url(request.getQueryString("url").get).get().map { r =>
   *     if (r.status == 200) Ok("The website is up") else NotFound("The website is down")
   *   }
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  final def async(block: R[B] => Future[Result]): Action[B] = async(parser)(block)

  /**
   * Constructs an `Action` with the given [[BodyParser]] that returns a future of a result.
   *
   * For example:
   * {{{
   * val hello = Action.async(parse.anyContent) { request =>
   *   ws.url(request.getQueryString("url").get).get().map { r =>
   *     if (r.status == 200) Ok("The website is up") else NotFound("The website is down")
   *   }
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  final def async[A](bodyParser: BodyParser[A])(block: R[A] => Future[Result]): Action[A] =
    composeAction(new Action[A] {
      def executionContext = self.executionContext
      def parser           = composeParser(bodyParser)
      def apply(request: Request[A]) =
        try {
          invokeBlock(request, block)
            .andThen { _ =>
              // This andThen block is used to keep a reference to the request until invokeBlock() is completed.
              // If the request contains TemporaryFiles, they will be deleted when they are garbage collected.
              // By keeping a reference to the request, it prevents the TemporaryFiles from becoming GC targets.
              request
            }(self.executionContext)
        } catch {
          // NotImplementedError is not caught by NonFatal, wrap it
          case e: NotImplementedError => throw new RuntimeException(e)
          // LinkageError is similarly harmless in Play Framework, since automatic reloading could easily trigger it
          case e: LinkageError => throw new RuntimeException(e)
        }
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

  override def andThen[Q[_]](other: ActionFunction[R, Q]): ActionBuilder[Q, B] = new ActionBuilder[Q, B] {
    def executionContext = self.executionContext
    def parser           = self.parser
    def invokeBlock[A](request: Request[A], block: Q[A] => Future[Result]) =
      self.invokeBlock[A](request, other.invokeBlock[A](_, block))
    protected override def composeParser[A](bodyParser: BodyParser[A]): BodyParser[A] = self.composeParser(bodyParser)
    protected override def composeAction[A](action: Action[A]): Action[A]             = self.composeAction(action)
  }
}

object ActionBuilder {
  class IgnoringBody()(implicit ec: ExecutionContext)
      extends ActionBuilderImpl(BodyParsers.utils.ignore[AnyContent](AnyContentAsEmpty))(ec)

  /**
   * An ActionBuilder that ignores the body passed into it. This uses the trampoline execution context, which
   * executes in the current thread. Since using this execution context in user code can cause unexpected
   * consequences, this method is private[play].
   */
  private[play] lazy val ignoringBody: ActionBuilder[Request, AnyContent] = new IgnoringBody()(Execution.trampoline)
}

/**
 * A trait representing the default action builder used by Play's controllers.
 *
 * This trait is used for binding, since some dependency injection frameworks doesn't deal
 * with types very well.
 */
trait DefaultActionBuilder extends ActionBuilder[Request, AnyContent]

object DefaultActionBuilder {
  def apply(parser: BodyParser[AnyContent])(implicit ec: ExecutionContext): DefaultActionBuilder =
    new DefaultActionBuilderImpl(parser)
}

class ActionBuilderImpl[B](val parser: BodyParser[B])(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[Request, B] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request)
}

class DefaultActionBuilderImpl(parser: BodyParser[AnyContent])(implicit ec: ExecutionContext)
    extends ActionBuilderImpl(parser)
    with DefaultActionBuilder {
  @Inject
  def this(parser: BodyParsers.Default)(implicit ec: ExecutionContext) = this(parser: BodyParser[AnyContent])
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
    refine(request).flatMap(_.fold(Future.successful, block))(executionContext)
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

  final def refine[A](request: R[A]): Future[Either[Result, P[A]]] = transform(request).map(Right(_))(executionContext)
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

  protected final def refine[A](request: R[A]) = filter(request).map(_.toLeft(request))(executionContext)
}
