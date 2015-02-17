/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.control.NonFatal
import play.api.libs.iteratee.Execution.Implicits.{ defaultExecutionContext => dec }
import play.api.libs.iteratee.internal.{ eagerFuture, executeFuture, executeIteratee, identityFunc, prepared }

/**
 * Various helper methods to construct, compose and traverse Iteratees.
 *
 * @define paramEcSingle @param ec The context to execute the supplied function with. The context is prepared on the calling thread before being used.
 * @define paramEcMultiple @param ec The context to execute the supplied functions with. The context is prepared on the calling thread before being used.
 */
object Iteratee {

  /**
   * flatten a [[scala.concurrent.Future]] of [[play.api.libs.iteratee.Iteratee]]] into an Iteratee
   *
   * @param i a promise of iteratee
   */
  def flatten[E, A](i: Future[Iteratee[E, A]]): Iteratee[E, A] = new FutureIteratee[E, A](i)

  def isDoneOrError[E, A](it: Iteratee[E, A]): Future[Boolean] = it.pureFoldNoEC { case Step.Cont(_) => false; case _ => true }

  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] which folds the content of the Input using a given function and an initial state
   *
   * Example:
   * {{{
   *   // Count the number of input elements
   *   def count[E]: Iteratee[E, Int] = Iteratee.fold(0)((c, _) => c + 1)
   * }}}
   *
   * @param state initial state
   * @param f a function folding the previous state and an input to a new state
   * $paramEcSingle
   */
  def fold[E, A](state: A)(f: (A, E) => A)(implicit ec: ExecutionContext): Iteratee[E, A] = foldM(state)((a, e: E) => eagerFuture(f(a, e)))(ec)

  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] which folds the content of the Input using a given function and an initial state
   *
   * M stands for Monadic which in this case means returning a [[scala.concurrent.Future]] for the function argument f,
   * so that promises are combined in a complete reactive flow of logic.
   *
   *
   * @param state initial state
   * @param f a function folding the previous state and an input to a new promise of state
   * $paramEcSingle
   */
  def foldM[E, A](state: A)(f: (A, E) => Future[A])(implicit ec: ExecutionContext): Iteratee[E, A] = {
    val pec = ec.prepare()
    def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {

      case Input.EOF => Done(s, Input.EOF)
      case Input.Empty => Cont[E, A](step(s))
      case Input.El(e) => { val newS = executeFuture(f(s, e))(pec); flatten(newS.map(s1 => Cont[E, A](step(s1)))(dec)) }
    }
    (Cont[E, A](step(state)))
  }

  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] which folds the content of the Input using a given function and an initial state.
   * Like `foldM`, but the fold can be completed earlier by returning a value of `true` in the future result.
   *
   * @param state initial state
   * @param f a function folding the previous state and an input to a promise of state and a boolean indicating whether the fold is done
   * $paramEcSingle
   */
  def fold2[E, A](state: A)(f: (A, E) => Future[(A, Boolean)])(implicit ec: ExecutionContext): Iteratee[E, A] = {
    val pec = ec.prepare()
    def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {

      case Input.EOF => Done(s, Input.EOF)
      case Input.Empty => Cont[E, A](step(s))
      case Input.El(e) => { val newS = executeFuture(f(s, e))(pec); flatten(newS.map[Iteratee[E, A]] { case (s1, done) => if (!done) Cont[E, A](step(s1)) else Done(s1, Input.Empty) }(dec)) }
    }
    (Cont[E, A](step(state)))
  }

  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] which folds the content of the Input using a given function and an initial state
   *
   * It also gives the opportunity to return a [[scala.concurrent.Future]] so that promises are combined in a complete reactive flow of logic.
   *
   *
   * @param state initial state
   * @param f a function folding the previous state and an input to a new promise of state
   * $paramEcSingle
   */
  def fold1[E, A](state: Future[A])(f: (A, E) => Future[A])(implicit ec: ExecutionContext): Iteratee[E, A] = {
    prepared(ec)(pec => flatten(state.map(s => foldM(s)(f)(pec))(dec)))
  }

  /**
   * A partially-applied function returned by the `consume` method.
   */
  trait Consume[E] {
    def apply[B, That]()(implicit t: E => TraversableOnce[B], bf: scala.collection.generic.CanBuildFrom[E, B, That]): Iteratee[E, That]
  }

  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] which consumes and concatenates all Input chunks
   *
   * Example:
   * {{{
   *   // Get all chunks of input
   *   def getAll: Iteratee[Array[Byte], Array[Byte]] = Iteratee.consume[Array[Byte]]()
   * }}}
   *
   * Chunks type should be viewable as TraversableOnce
   *
   */
  def consume[E] = new Consume[E] {
    def apply[B, That]()(implicit t: E => TraversableOnce[B], bf: scala.collection.generic.CanBuildFrom[E, B, That]): Iteratee[E, That] = {
      fold[E, Seq[E]](Seq.empty) { (els, chunk) =>
        chunk +: els
      }(dec).map { elts =>
        val builder = bf()
        elts.reverse.foreach(builder ++= _)
        builder.result()
      }(dec)
    }
  }

  /**
   * Create an iteratee that takes the first element of the stream, if one occurs before EOF
   */
  def head[E]: Iteratee[E, Option[E]] = {

    def step: K[E, Option[E]] = {
      case Input.Empty => Cont(step)
      case Input.EOF => Done(None, Input.EOF)
      case Input.El(e) => Done(Some(e), Input.Empty)
    }
    Cont(step)
  }

  /**
   * Consume all the chunks from the stream, and return a list.
   */
  def getChunks[E]: Iteratee[E, List[E]] = fold[E, List[E]](Nil) { (els, chunk) => chunk +: els }(dec).map(_.reverse)(dec)

  /**
   * Read up to n chunks from the stream stopping when that number of chunks have
   * been read or the stream end is reached. If the stream has fewer elements then
   * only those elements are returned. Will consume intermediate Input.Empty elements
   * but does not consume Input.EOF.
   */
  def takeUpTo[E](n: Int): Iteratee[E, Seq[E]] = {
    def stepWith(accum: Seq[E]): Iteratee[E, Seq[E]] = {
      if (accum.length >= n) Done(accum) else Cont {
        case Input.EOF =>
          Done(accum, Input.EOF)
        case Input.Empty =>
          stepWith(accum)
        case Input.El(el) =>
          stepWith(accum :+ el)
      }
    }
    stepWith(Seq.empty)
  }

  /**
   * Determines whether or not a stream contains any elements. A stream can be
   * empty if it has no inputs (except Input.EOF) or if it consists of only Input.Empty
   * elements (and Input.EOF.) A stream is non-empty if it contains an Input.El.
   *
   * This iteratee consumes the stream as far as the first EOF or Input.El, skipping
   * over any Input.Empty elements. When it encounters an Input.EOF or Input.El it
   * is Done.
   *
   * Will consume intermediate Input.Empty elements but does not consume Input.El or
   * Input.EOF.
   */
  def isEmpty[E]: Iteratee[E, Boolean] = Cont {
    case Input.EOF =>
      Done(true, Input.EOF)
    case Input.Empty =>
      isEmpty[E]
    case input @ Input.El(_) =>
      Done(false, input)
  }

  /**
   * Ignore all the input of the stream, and return done when EOF is encountered.
   */
  def skipToEof[E]: Iteratee[E, Unit] = {
    def cont: Iteratee[E, Unit] = Cont {
      case Input.EOF => Done((), Input.EOF)
      case _ => cont
    }
    cont
  }

  /**
   * A partially-applied function returned by the `eofOrElse` method.
   */
  trait EofOrElse[E] {
    /**
     * @param otherwise Value if the input is not [[play.api.libs.iteratee.Input.EOF]]
     * @param eofValue Value if the input is [[play.api.libs.iteratee.Input.EOF]]
     * @tparam A Type of `eofValue`
     * @tparam B Type of `otherwise`
     * @return An `Iteratee[E, Either[B, A]]` that consumes one input and produces a `Right(eofValue)` if this input is [[play.api.libs.iteratee.Input.EOF]] otherwise it produces a `Left(otherwise)`
     */
    def apply[A, B](otherwise: B)(eofValue: A): Iteratee[E, Either[B, A]]
  }

  def eofOrElse[E] = new EofOrElse[E] {
    def apply[A, B](otherwise: B)(eofValue: A): Iteratee[E, Either[B, A]] = {
      def cont: Iteratee[E, Either[B, A]] = Cont((in: Input[E]) => {
        in match {
          case Input.El(e) => Done(Left(otherwise), in)
          case Input.EOF => Done(Right(eofValue), in)
          case Input.Empty => cont
        }
      })
      cont
    }
  }

  /**
   * @return an [[play.api.libs.iteratee.Iteratee]] which just ignores its input
   */
  def ignore[E]: Iteratee[E, Unit] = fold[E, Unit](())((_, _) => ())(dec)

  /**
   * @return an [[play.api.libs.iteratee.Iteratee]] which executes a provided function for every chunk. Returns Done on EOF.
   *
   * Example:
   * {{{
   *   // Get all chunks of input
   *   def printChunks: Iteratee[String, Unit] = Iteratee.foreach[String]( s => println(s) )
   * }}}
   *
   * @param f the function that should be executed for every chunk
   */
  def foreach[E](f: E => Unit)(implicit ec: ExecutionContext): Iteratee[E, Unit] = fold[E, Unit](())((_, e) => f(e))(ec)

  /**
   *
   * @return an [[play.api.libs.iteratee.Iteratee]] which pushes the input into the provided [[play.api.libs.iteratee.Iteratee]], starting over again each time it terminates until an EOF is received, collecting a sequence of results of the different use of the iteratee
   *
   * @param i an iteratee used repeatedly to compute a sequence of results
   */
  def repeat[E, A](i: Iteratee[E, A]): Iteratee[E, Seq[A]] = {

    def step(s: Seq[A])(input: Input[E]): Iteratee[E, Seq[A]] = {
      input match {
        case Input.EOF => Done(s, Input.EOF)

        case Input.Empty => Cont(step(s))

        case Input.El(e) => i.pureFlatFold {
          case Step.Done(a, e) => Done(s :+ a, input)
          case Step.Cont(k) => k(input).flatMap(a => repeat(i).map(az => s ++ (a +: az))(dec))(dec)
          case Step.Error(msg, e) => Error(msg, e)
        }(dec)
      }
    }

    Cont(step(Seq.empty[A]))

  }

}

/**
 * Input that can be consumed by an iteratee
 */
sealed trait Input[+E] {
  def map[U](f: (E => U)): Input[U] = this match {
    case Input.El(e) => Input.El(f(e))
    case Input.Empty => Input.Empty
    case Input.EOF => Input.EOF
  }
}

object Input {

  /**
   * An input element
   */
  case class El[+E](e: E) extends Input[E]

  /**
   * An empty input
   */
  case object Empty extends Input[Nothing]

  /**
   * An end of file input
   */
  case object EOF extends Input[Nothing]

}

/**
 * Represents the state of an iteratee.
 */
sealed trait Step[E, +A] {

  // This version is not called by Step implementations in Play,
  // but could be called by custom implementations.
  def it: Iteratee[E, A] = this match {
    case Step.Done(a, e) => Done(a, e)
    case Step.Cont(k) => Cont(k)
    case Step.Error(msg, e) => Error(msg, e)
  }

}

object Step {

  /**
   * A done state of an iteratee
   *
   * @param a The value that the iteratee has consumed
   * @param remaining The remaining input that the iteratee received but didn't consume
   */
  case class Done[+A, E](a: A, remaining: Input[E]) extends Step[E, A]

  /**
   * A continuing state of an iteratee.
   *
   * @param k A function that can receive input for the iteratee to process.
   */
  case class Cont[E, +A](k: Input[E] => Iteratee[E, A]) extends Step[E, A]

  /**
   * An error state of an iteratee
   *
   * @param msg The error message
   * @param input The remaining input that the iteratee received but didn't consume
   */
  case class Error[E](msg: String, input: Input[E]) extends Step[E, Nothing]
}

/**
 * An Iteratee consumes a stream of elements of type E, producing a result of type A.
 * The stream itself is represented by the Input trait. An Iteratee is an immutable
 * data type, so each step in consuming the stream generates a new Iteratee with a new
 * state.
 *
 * At a high level, an Iteratee is just a function that takes a piece of input and
 * returns either a final result or a new function that takes another piece of input.
 * To represent this, an Iteratee can be in one of three states
 * (see the [[play.api.libs.iteratee.Step]] trait):
 * [[play.api.libs.iteratee.Done]], which means it contains a result and potentially some unconsumed part of the stream;
 * [[play.api.libs.iteratee.Cont]], which means it contains a function to be invoked to generate a new Iteratee from the next piece of input;
 * [[play.api.libs.iteratee.Error]], which means it contains an error message and potentially some unconsumed part of the stream.
 *
 * One would expect to transform an Iteratee through the Cont state N times, eventually
 * arriving at either the Done or Error state.
 *
 * Typically an [[play.api.libs.iteratee.Enumerator]] would be used to
 * push data into an Iteratee by invoking the function in the [[play.api.libs.iteratee.Cont]]
 * state until either 1) the iteratee leaves the Cont state or 2) the enumerator
 * runs out of data.
 *
 * The Iteratee does not do any resource management (such as closing streams);
 * the producer pushing stuff into the Iteratee has that responsibility.+ *
 * The state of an Iteratee (the current [[play.api.libs.iteratee.Step]] may not be available
 * synchronously; it may be pending an asynchronous computation. This is the difference
 * between Iteratee and Step.
 * @tparam E Input type
 * @tparam A Result type of this Iteratee
 *
 * @define paramEcSingle @param ec The context to execute the supplied function with. The context is prepared on the calling thread.
 * @define paramEcMultiple @param ec The context to execute the supplied functions with. The context is prepared on the calling thread.
 */
trait Iteratee[E, +A] {
  self =>

  /**
   * Extracts the computed result of the Iteratee pushing an Input.EOF if necessary
   * Extracts the computed result of the Iteratee, pushing an Input.EOF first
   * if the Iteratee is in the [[play.api.libs.iteratee.Cont]] state.
   * In case of error, an exception may be thrown synchronously or may
   * be used to complete the returned Promise; this indeterminate behavior
   * is inherited from fold().
   *
   *  @return a [[scala.concurrent.Future]] of the eventually computed result
   */
  def run: Future[A] = fold({
    case Step.Done(a, _) => Future.successful(a)
    case Step.Cont(k) => k(Input.EOF).fold({
      case Step.Done(a1, _) => Future.successful(a1)
      case Step.Cont(_) => sys.error("diverging iteratee after Input.EOF")
      case Step.Error(msg, e) => sys.error(msg)
    })(dec)
    case Step.Error(msg, e) => sys.error(msg)
  })(dec)

  /**
   * Sends one element of input to the Iteratee and returns a promise
   * containing the new Iteratee. The promise may or may not be completed
   * already when it's returned (the iteratee may use an asynchronous operation to handle
   * the input).
   * @param in input being sent
   */
  def feed[AA >: A](in: Input[E]): Future[Iteratee[E, AA]] = {
    Enumerator.enumInput(in) |>> this
  }

  /**
   * Converts the Iteratee into a Promise containing its state.
   */
  def unflatten: Future[Step[E, A]] = pureFold(identity)(dec)

  /**
   *
   * This method provides the means to check on the state of the Iteratee and eventually extract a value in a Promise
   * @param done a function that will be called if the Iteratee is a Done
   * @param cont a function that will be called if the Iteratee is a Cont
   * @param error a function that will be called if the Iteratee is an Error
   * $paramEcMultiple
   * @return a [[scala.concurrent.Future]] of a value extracted by calling the appropriate provided function
   */
  def fold1[B](done: (A, Input[E]) => Future[B],
    cont: (Input[E] => Iteratee[E, A]) => Future[B],
    error: (String, Input[E]) => Future[B])(implicit ec: ExecutionContext): Future[B] = fold({
    case Step.Done(a, e) => done(a, e)
    case Step.Cont(k) => cont(k)
    case Step.Error(msg, e) => error(msg, e)
  })(ec)

  /**
   * Computes a promised value B from the state of the Iteratee.
   *
   * The folder function will be run in the supplied ExecutionContext.
   * Exceptions thrown by the folder function will be stored in the
   * returned Promise.
   *
   * If the folder function itself is synchronous, it's better to
   * use `pureFold()` instead of `fold()`.
   *
   * @param folder a function that will be called on the current state of the iteratee
   * @param ec the ExecutionContext to run folder within
   * @return the result returned when folder is called
   */
  def fold[B](folder: Step[E, A] => Future[B])(implicit ec: ExecutionContext): Future[B]

  /**
   * A version of `fold` that runs `folder` in the current thread rather than in a
   * supplied ExecutionContext, called in several places where we are sure the stack
   * cannot overflow. This method is designed to be overridden by `StepIteratee`,
   * which can execute the `folder` function immediately.
   */
  protected[play] def foldNoEC[B](folder: Step[E, A] => Future[B]): Future[B] =
    fold(folder)(dec)

  /**
   * Like fold but taking functions returning pure values (not in promises)
   *
   * @return a [[scala.concurrent.Future]] of a value extracted by calling the appropriate provided function
   */
  def pureFold[B](folder: Step[E, A] => B)(implicit ec: ExecutionContext): Future[B] = fold(s => eagerFuture(folder(s)))(ec) // Use eagerFuture because fold will ensure folder is run in ec

  /**
   * A version of `pureFold` that runs `folder` in the current thread rather than in a
   * supplied ExecutionContext, called in several places where we are sure the stack
   * cannot overflow. This method is designed to be overridden by `StepIteratee`,
   * which can execute the `folder` function immediately.
   */
  protected[play] def pureFoldNoEC[B](folder: Step[E, A] => B): Future[B] =
    pureFold(folder)(dec)

  /**
   * Like pureFold, except taking functions that return an Iteratee
   *
   * @return an Iteratee extracted by calling the appropriate provided function
   */
  def pureFlatFold[B, C](folder: Step[E, A] => Iteratee[B, C])(implicit ec: ExecutionContext): Iteratee[B, C] = Iteratee.flatten(pureFold(folder)(ec))

  /**
   * A version of `pureFlatFold` that runs `folder` in the current thread rather than in a
   * supplied ExecutionContext, called in several places where we are sure the stack
   * cannot overflow. This method is designed to be overridden by `StepIteratee`,
   * which can execute the `folder` function immediately.
   */
  protected[play] def pureFlatFoldNoEC[B, C](folder: Step[E, A] => Iteratee[B, C]): Iteratee[B, C] =
    pureFlatFold(folder)(dec)

  /**
   * Like fold, except flattens the result with Iteratee.flatten.
   *
   * $paramEcSingle
   */
  def flatFold0[B, C](folder: Step[E, A] => Future[Iteratee[B, C]])(implicit ec: ExecutionContext): Iteratee[B, C] = Iteratee.flatten(fold(folder)(ec))

  /**
   * Like fold1, except flattens the result with Iteratee.flatten.
   *
   * $paramEcSingle
   */
  def flatFold[B, C](done: (A, Input[E]) => Future[Iteratee[B, C]],
    cont: (Input[E] => Iteratee[E, A]) => Future[Iteratee[B, C]],
    error: (String, Input[E]) => Future[Iteratee[B, C]])(implicit ec: ExecutionContext): Iteratee[B, C] = Iteratee.flatten(fold1(done, cont, error)(ec))

  /**
   *
   * Uses the provided function to transform the Iteratee's computed result when the Iteratee is done.
   *
   * @param f a function for transforming the computed result
   * $paramEcSingle
   */
  def map[B](f: A => B)(implicit ec: ExecutionContext): Iteratee[E, B] = this.flatMap(a => Done(f(a), Input.Empty))(ec)

  /**
   * Like map but allows the map function to execute asynchronously.
   *
   * This is particularly useful if you want to do blocking operations, so that you can ensure that those operations
   * execute in the right execution context, rather than the iteratee execution context, which would potentially block
   * all other iteratee operations.
   *
   * @param f a function for transforming the computed result
   * $paramEcSingle
   */
  def mapM[B](f: A => Future[B])(implicit ec: ExecutionContext): Iteratee[E, B] = self.flatMapM(a => f(a).map[Iteratee[E, B]](b => Done(b))(dec))(ec)

  /**
   * On Done of this Iteratee, the result is passed to the provided function, and the resulting Iteratee is used to continue consuming input
   *
   * If the resulting Iteratee of evaluating the f function is a Done then its left Input is ignored and its computed result is wrapped in a Done and returned
   *
   * @param f a function for transforming the computed result into an Iteratee
   * $paramEcSingle
   */
  def flatMap[B](f: A => Iteratee[E, B])(implicit ec: ExecutionContext): Iteratee[E, B] = {
    self.pureFlatFoldNoEC { // safe: folder either yields value immediately or executes with another EC
      case Step.Done(a, Input.Empty) => executeIteratee(f(a))(ec /* still on same thread; let executeIteratee do preparation */ )
      case Step.Done(a, e) => executeIteratee(f(a))(ec /* still on same thread; let executeIteratee do preparation */ ).pureFlatFold {
        case Step.Done(a, _) => Done(a, e)
        case Step.Cont(k) => k(e)
        case Step.Error(msg, e) => Error(msg, e)
      }(dec)
      case Step.Cont(k) => {
        implicit val pec = ec.prepare()
        Cont((in: Input[E]) => executeIteratee(k(in))(dec).flatMap(f)(pec))
      }
      case Step.Error(msg, e) => Error(msg, e)
    }
  }

  /**
   * Like flatMap but allows the flatMap function to execute asynchronously.
   *
   * This is particularly useful if you want to do blocking operations, so that you can ensure that those operations
   * execute in the right execution context, rather than the iteratee execution context, which would potentially block
   * all other iteratee operations.
   *
   * @param f a function for transforming the computed result into an Iteratee
   * $paramEcSingle
   */
  def flatMapM[B](f: A => Future[Iteratee[E, B]])(implicit ec: ExecutionContext): Iteratee[E, B] = self.flatMap(a => Iteratee.flatten(f(a)))(ec)

  def flatMapInput[B](f: Step[E, A] => Iteratee[E, B])(implicit ec: ExecutionContext): Iteratee[E, B] = self.pureFlatFold(f)(ec)

  /**
   * Like flatMap except that it concatenates left over inputs if the Iteratee returned by evaluating f is a Done.
   *
   * @param f a function for transforming the computed result into an Iteratee
   * $paramEcSingle Note: input concatenation is performed in the iteratee default execution context, not in the user-supplied context.
   */
  def flatMapTraversable[B, X](f: A => Iteratee[E, B])(implicit p: E => scala.collection.TraversableLike[X, E], bf: scala.collection.generic.CanBuildFrom[E, X, E], ec: ExecutionContext): Iteratee[E, B] = {
    val pec = ec.prepare()
    self.pureFlatFold {
      case Step.Done(a, Input.Empty) => f(a)
      case Step.Done(a, e) => executeIteratee(f(a))(pec).pureFlatFold {
        case Step.Done(a, eIn) => {
          val fullIn = (e, eIn) match {
            case (Input.Empty, in) => in
            case (in, Input.Empty) => in
            case (Input.EOF, _) => Input.EOF
            case (in, Input.EOF) => in
            case (Input.El(e1), Input.El(e2)) => Input.El[E](p(e1) ++ p(e2))
          }

          Done(a, fullIn)
        }
        case Step.Cont(k) => k(e)
        case Step.Error(msg, e) => Error(msg, e)
      }(dec)
      case Step.Cont(k) => Cont((in: Input[E]) => k(in).flatMap(f)(pec))
      case Step.Error(msg, e) => Error(msg, e)
    }(dec)
  }

  /**
   * Creates a new Iteratee that will handle any matching exception the original Iteratee may contain. This lets you
   * provide a fallback value in case your Iteratee ends up in an error state.
   *
   * Example:
   *
   * {{{
   * def it = Iteratee.map(i => 10 / i).recover { case t: Throwable =>
   *   Logger.error("Must have divided by zero!", t)
   *   Integer.MAX_VALUE
   * }
   *
   * Enumerator(5).run(it) // => 2
   * Enumerator(0).run(it) // => returns Integer.MAX_VALUE and logs "Must have divied by zero!"
   * }}}
   *
   * @param pf
   * @param ec
   * @tparam B
   * @return
   */
  def recover[B >: A](pf: PartialFunction[Throwable, B])(implicit ec: ExecutionContext): Iteratee[E, B] = {
    recoverM { case t: Throwable if pf.isDefinedAt(t) => Future.successful(pf(t)) }(ec)
  }

  /**
   * A version of `recover` that allows the partial function to return a Future[B] instead of B.
   *
   * @param pf
   * @param ec
   * @tparam B
   * @return
   */
  def recoverM[B >: A](pf: PartialFunction[Throwable, Future[B]])(implicit ec: ExecutionContext): Iteratee[E, B] = {
    val pec = ec.prepare()
    recoverWith { case t: Throwable if pf.isDefinedAt(t) => Iteratee.flatten(pf(t).map(b => Done[E, B](b))(pec)) }(ec)
  }

  /**
   * A version of `recover` that allows the partial function to return an Iteratee[E, B] instead of B.
   *
   * @param pf
   * @param ec
   * @tparam B
   * @return
   */
  def recoverWith[B >: A](pf: PartialFunction[Throwable, Iteratee[E, B]])(implicit ec: ExecutionContext): Iteratee[E, B] = {
    implicit val pec = ec.prepare()

    def recoveringIteratee(it: Iteratee[E, A]): Iteratee[E, B] = {
      val futureRecoveringIteratee: Future[Iteratee[E, B]] = it.pureFlatFold[E, B] {
        case Step.Cont(k) =>
          Cont { input: Input[E] =>
            val orig: Iteratee[E, A] = k(input)
            val recovering: Iteratee[E, B] = recoveringIteratee(orig)
            recovering
          }
        case Step.Error(msg, _) =>
          throw new IterateeException(msg)
        case done => done.it
      }(dec).unflatten.map(_.it)(dec).recover(pf)(pec)
      Iteratee.flatten(futureRecoveringIteratee)
    }

    recoveringIteratee(this)
  }

  def joinI[AIn](implicit in: A <:< Iteratee[_, AIn]): Iteratee[E, AIn] = {
    this.flatMap { a =>
      val inner = in(a)
      inner.pureFlatFold[E, AIn] {
        case Step.Done(a, _) => Done(a, Input.Empty)
        case Step.Cont(k) => k(Input.EOF).pureFlatFold[E, AIn] {
          case Step.Done(a, _) => Done(a, Input.Empty)
          case Step.Cont(k) => Error("divergent inner iteratee on joinI after EOF", Input.EOF)
          case Step.Error(msg, e) => Error(msg, Input.EOF)
        }(dec)
        case Step.Error(msg, e) => Error(msg, Input.Empty)
      }(dec)
    }(dec)
  }

  def joinConcatI[AIn, X](implicit in: A <:< Iteratee[E, AIn], p: E => scala.collection.TraversableLike[X, E], bf: scala.collection.generic.CanBuildFrom[E, X, E]): Iteratee[E, AIn] = {
    this.flatMapTraversable { a =>
      val inner = in(a)
      inner.pureFlatFold[E, AIn] {
        case Step.Done(a, e) => Done(a, e)
        case Step.Cont(k) => k(Input.EOF).pureFlatFold[E, AIn] {
          case Step.Done(a, e) => Done(a, e)
          case Step.Cont(k) => Error("divergent inner iteratee on joinI after EOF", Input.EOF)
          case Step.Error(msg, e) => Error(msg, Input.EOF)
        }(dec)
        case Step.Error(msg, e) => Error(msg, Input.Empty)
      }(dec)
    }
  }
}

/**
 * An iteratee that already knows its own state, vs [[FutureIteratee]].
 * Several performance improvements are possible when an iteratee's
 * state is immediately available.
 */
private sealed trait StepIteratee[E, A] extends Iteratee[E, A] with Step[E, A] {

  final override def it: Iteratee[E, A] = this
  final def immediateUnflatten: Step[E, A] = this

  final override def unflatten: Future[Step[E, A]] = Future.successful(immediateUnflatten)

  final override def fold[B](folder: Step[E, A] => Future[B])(implicit ec: ExecutionContext): Future[B] = {
    executeFuture {
      folder(immediateUnflatten)
    }(ec /* executeFuture handles preparation */ )
  }

  final override def pureFold[B](folder: Step[E, A] => B)(implicit ec: ExecutionContext): Future[B] = {
    Future {
      folder(immediateUnflatten)
    }(ec /* Future.apply handles preparation */ )
  }

  protected[play] final override def foldNoEC[B](folder: Step[E, A] => Future[B]): Future[B] = {
    folder(immediateUnflatten)
  }

  protected[play] final override def pureFoldNoEC[B](folder: Step[E, A] => B): Future[B] = {
    try Future.successful(folder(immediateUnflatten)) catch { case NonFatal(e) => Future.failed(e) }
  }

  protected[play] final override def pureFlatFoldNoEC[B, C](folder: Step[E, A] => Iteratee[B, C]): Iteratee[B, C] = {
    folder(immediateUnflatten)
  }

}

/**
 * An iteratee in the "done" state.
 */
private final class DoneIteratee[E, A](a: A, e: Input[E]) extends Step.Done[A, E](a, e) with StepIteratee[E, A] {

  /**
   * Use an optimized implementation because this method is called by Play when running an
   * Action over a BodyParser result.
   */
  override def mapM[B](f: A => Future[B])(implicit ec: ExecutionContext): Iteratee[E, B] = {
    Iteratee.flatten(executeFuture {
      f(a).map[Iteratee[E, B]](Done(_, e))(dec)
    }(ec /* delegate preparation */ ))
  }

}

/**
 * An iteratee in the "cont" state.
 */
private final class ContIteratee[E, A](k: Input[E] => Iteratee[E, A]) extends Step.Cont[E, A](k) with StepIteratee[E, A] {
}

/**
 * An iteratee in the "error" state.
 */
private final class ErrorIteratee[E](msg: String, e: Input[E]) extends Step.Error[E](msg, e) with StepIteratee[E, Nothing] {
}

/**
 * An iteratee whose state is provided in a Future, vs [[StepIteratee]].
 * Used by `Iteratee.flatten`.
 */
private final class FutureIteratee[E, A](itFut: Future[Iteratee[E, A]]) extends Iteratee[E, A] {

  def fold[B](folder: Step[E, A] => Future[B])(implicit ec: ExecutionContext): Future[B] = {
    implicit val pec = ec.prepare()
    itFut.flatMap { it => it.fold(folder)(pec) }(dec)
  }

}

object Done {
  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] in the “done” state.
   * @param a Result
   * @param e Remaining unused input
   */
  def apply[E, A](a: A, e: Input[E] = Input.Empty): Iteratee[E, A] = new DoneIteratee[E, A](a, e)
}

object Cont {
  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] in the “cont” state.
   * @param k Continuation which will compute the next Iteratee state according to an input. The continuation is not
   * guaranteed to run in any particular execution context. If a particular execution context is needed then the
   * continuation should be wrapped before being added, e.g. by running the operation in a Future and flattening the
   * result.
   */
  def apply[E, A](k: Input[E] => Iteratee[E, A]): Iteratee[E, A] = new ContIteratee[E, A](k)
}

object Error {
  /**
   * Create an [[play.api.libs.iteratee.Iteratee]] in the “error” state.
   * @param msg Error message
   * @param e The input that caused the error
   */
  def apply[E](msg: String, e: Input[E]): Iteratee[E, Nothing] = new ErrorIteratee[E](msg, e)
}

/**
 * An Exception that represents an Iteratee that ended up in an Error state with the given
 * error message. This exception will eventually be removed and replaced with
 * `IterateeException` (notice the extra `c`).
 */
@deprecated("Use IterateeException instead (notice the extra 'c')", "2.4.0")
class IterateeExeption(msg: String) extends Exception(msg)

/**
 * An Exception that represents an Iteratee that ended up in an Error state with the given
 * error message.
 */
class IterateeException(msg: String) extends IterateeExeption(msg)
