package play.api.libs.concurrent

import scala.language.higherKinds

import play.core._
import play.api._

import scala.concurrent.duration.{ FiniteDuration, Duration }

import java.util.concurrent.{ TimeUnit }

import scala.concurrent.{ Future, ExecutionContext, Promise => SPromise }
import scala.collection.mutable.Builder
import scala.collection._
import scala.collection.generic.CanBuildFrom
import java.util.concurrent.TimeoutException
import play.core.Execution.internalContext
import scala.util.Try
import scala.util.control.NonFatal

/**
 * The state of a promise; it's waiting, contains a value, or contains an exception.
 */
@scala.deprecated("Use scala.concurrent.Promise instead.", "2.2")
sealed trait PromiseValue[+A] {

  /**
   * true if the promise has either a value or an exception.
   */
  def isDefined = this match { case Waiting => false; case _ => true }
}

/**
 * A promise state that contains either a value or an exception.
 */
@scala.deprecated("Use scala.util.Try instead.", "2.2")
trait NotWaiting[+A] extends PromiseValue[A] {

  /**
   * Return the value or the promise, or throw it if it held an exception.
   */
  def get: A = this match {
    case Thrown(e) => throw e
    case Redeemed(a) => a
  }

  /**
   * Invoke either onError with the promise's exception or onSuccess with its value.
   * @param onError contains function that's executed in case of error
   * @param onSuccess contains function that's executed in case of success
   */
  def fold[B](onError: Throwable => B, onSuccess: A => B): B = this match {
    case Thrown(e) => onError(e)
    case Redeemed(r) => onSuccess(r)
  }

}

/**
 * A promise state containing an exception.
 */
@scala.deprecated("Use scala.util.Failure instead.", "2.2")
case class Thrown(e: scala.Throwable) extends NotWaiting[Nothing]

/**
 * A promise state containing a non-exception value.
 */
@scala.deprecated("Use scala.util.Success instead.", "2.2")
case class Redeemed[+A](a: A) extends NotWaiting[A]

/**
 * A promise state indicating it has not been completed yet.
 */
case object Waiting extends PromiseValue[Nothing]

class PlayRedeemable[-A](p: scala.concurrent.Promise[A]) extends Redeemable[A] {

  def redeem(a: => A)(implicit ec: ExecutionContext): Unit = p.completeWith(Future(a)(ec))

  def throwing(t: Throwable): Unit = p.failure(t)

}

/**
 * A promised value that wraps around scala.concurent.Future.
 * The value of the promise is to be computed asynchronously.
 * The promise will be completed either with the expected value type or with
 * an exception.
 */
@scala.deprecated("Use scala.concurrent.Promise instead.", "2.2")
class PlayPromise[+A](fu: scala.concurrent.Future[A]) {

  /**
   * Registers a callback to be invoked when (and if) the promise
   * is completed with a non-exception value. The callback may
   * run in another thread.
   * @param k the callback
   */
  def onRedeem(k: A => Unit)(implicit ec: ExecutionContext): Unit = extend1 { case Redeemed(a) => k(a); case _ => }

  /**
   * Creates a new Promise[B], by running the function k on this promise
   * after it is redeemed with a value or exception. The function may
   * run in another thread. extend() is similar to map() but can modify
   * an exception contained in the promise, not only a successful value.
   * @param k function to be executed on this promise
   */
  def extend[B](k: Function1[Future[A], B])(implicit ec: ExecutionContext): Future[B] = {
    val result = Promise[B]()
    fu.onComplete(_ => result.redeem(k(fu))(ec))(ec)
    result.future
  }

  /**
   * Like extend() but gives your function the promise's value rather
   * than the promise itself.
   * @param k function to be executed on this promise
   */
  def extend1[B](k: Function1[NotWaiting[A], B])(implicit ec: ExecutionContext): Future[B] = extend[B](p => k(p.value.get match { case scala.util.Failure(e) => Thrown(e); case scala.util.Success(a) => Redeemed(a) }))(ec)

  /**
   * Synonym for await, blocks for the promise to be completed with a value or
   * an exception.
   */
  def value1 = await

  /**
   * Blocks for Promise.defaultTimeout until the promise has a value or an exception;
   * throws java.util.concurrent.TimeoutException if the promise is not redeemed within the default timeout period.
   * Unlike Await.result() in Akka, does NOT throw an exception from the Promise itself.
   */
  def await: NotWaiting[A] = await(Promise.defaultTimeout)

  /**
   * Blocks for a specified timeout until the promise has a value or an exception;
   * throws java.util.concurrent.TimeoutException if the promise is not redeemed in
   * time. Unlike Await.result() in Akka, does NOT throw an exception from the Promise itself.
   */
  def await(timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): NotWaiting[A] = {
    scala.concurrent.Await.ready(fu, scala.concurrent.duration.Duration(timeout, unit))
    fu.value.get match { case scala.util.Failure(e) => Thrown(e); case scala.util.Success(a) => Redeemed(a) }
  }

  /**
   * Computes a new Promise which will be completed with NoSuchElementException
   * if the value of this promise does not match the given predicate.
   * The predicate may be run in another thread.
   * @param p predicate
   */

  def filter(p: A => Boolean): Future[A] = null

  /**
   * Returns a Promise representing the first-completed of this promise
   * or another promise. The resulting Promise contains either
   * Left(resultOfThisPromise), Right(resultOfOtherPromise), or
   * an exception which could be from either of the two promises.
   * @param other promise to be composed with
   */
  def or[B](other: Future[B]): Future[Either[A, B]] = {
    import scala.concurrent.stm._

    val p = Promise[Either[A, B]]()
    val ref = Ref(false)
    this.extend1 { v =>
      if (!ref.single()) {
        val iRedeemed = ref.single.getAndTransform(_ => true)

        if (!iRedeemed) {
          v match {
            case Redeemed(a) =>
              p.redeem(Left(a))(internalContext)
            case Thrown(e) =>
              p.throwing(e)
          }
        }
      }
    }(internalContext)
    other.extend1 { v =>
      if (!ref.single()) {
        val iRedeemed = ref.single.getAndTransform(_ => true)

        if (!iRedeemed) {
          v match {
            case Redeemed(a) =>
              p.redeem(Right(a))(internalContext)
            case Thrown(e) =>
              p.throwing(e)
          }
        }
      }
    }(internalContext)
    p.future
  }

  /**
   * Creates a scheduled promise with the given message.  Message a is shown if the scheduled promise
   * redeems before the current promise
   * @param message message to be displayed if the scheduled promise redeems first
   * @param duration duration
   * @param unti time unit
   * @return either the scheduled message or the current promise
   */
  def orTimeout[B](message: => B, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS)(implicit ec: ExecutionContext): Future[Either[A, B]] = {
    or(Promise.timeout(message, duration, unit)(ec))
  }

  /**
   * Creates a scheduled promise with the given message (using the deafult Promise timeout).
   * Message a is shown if the scheduled promise redeems before the current promise
   * @param message message to be displayed if the scheduled promise redeems first
   * @param duration duration
   * @param unti time unit
   * @return either the scheduled message or the current promise
   */

  def orTimeout[B](message: B): Future[Either[A, B]] = orTimeout(message, Promise.defaultTimeout)(internalContext)

  /**
   * Creates a scheduled promise with  Throwable e (using the deafult Promise timeout).
   * Exception e is shown if scheduled promise redeemds first
   * @param e exception to be thrown
   * @return a Promise which may throw an exception
   */
  def orTimeout(e: Throwable): Future[A] = orTimeout(e, Promise.defaultTimeout)(internalContext).map(_.fold(a => a, e => throw e))(internalContext)

}

/**
 * A redeemable can be completed exactly once with either a value of type A or an exception.
 */
@scala.deprecated("Use scala.concurrent.Promise instead.", "2.2")
trait Redeemable[-A] {

  /**
   * Complete the redeemable with a value.
   * May only be called one time, and may not be called if throwing() was also called.
   * If evaluating the value throws an exception, equivalent to calling throwing() on that exception.
   */
  def redeem(a: => A)(implicit ec: ExecutionContext): Unit

  /**
   * Complete the redeemable with an exception.
   * May only be called one time, and may not be called if redeem() was also called.
   */
  def throwing(t: Throwable): Unit
}

/**
 * Represents an already-completed promise by immediately
 * evaluating the parameter. Catches any exception from evaluating its parameter and
 * completes the promise with that exception. The promise is "pure" because it could
 * be just a pure value, the Promise wrapper is superflous.
 */
@scala.deprecated("Use scala.concurrent.Promise instead.", "2.2")
object PurePromise {

  /**
   * factory method for a pure promise
   */
  @scala.deprecated("Use scala.concurrent.Promise.complete(Try(...)) instead.", "2.2")
  def apply[A](lazyA: => A): scala.concurrent.Future[A] = (try (scala.concurrent.Promise.successful(lazyA)) catch {
    case NonFatal(t) => scala.concurrent.Promise.failed(t)
  }).future

}

/**
 * useful helper methods to create and compose Promises
 */
object Promise {

  private[play] lazy val defaultTimeout =
    //TODO get it from conf
    Duration(10000, TimeUnit.MILLISECONDS).toMillis

  /**
   * Synonym for PurePromise.apply
   */
  @scala.deprecated("Use scala.concurrent.Future.successful() or scala.concurrent.Promise.complete(Try(...)) instead.", "2.2")
  def pure[A](a: => A): Future[A] = PurePromise(a)

  /**
   * Constructs a new redeemable Promise which has not been redeemed yet.
   */
  @scala.deprecated("Use scala.concurrent.Promise() instead.", "2.2")
  def apply[A](): scala.concurrent.Promise[A] = scala.concurrent.Promise[A]()

  /**
   * Constructs a promise which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises
   * @param message message to be displayed
   * @param duration duration for the scheduled promise
   * @return a scheduled promise
   */
  def timeout[A](message: => A, duration: scala.concurrent.duration.Duration)(implicit ec: ExecutionContext): Future[A] = {
    timeout(message, duration.toMillis)
  }

  /**
   * Constructs a promise which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises
   * @param message message to be displayed
   * @param duration duration for the scheduled promise
   * @return a scheduled promise
   */
  def timeout[A](message: => A, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS)(implicit ec: ExecutionContext): Future[A] = {
    val p = SPromise[A]()
    import play.api.Play.current
    Akka.system.scheduler.scheduleOnce(FiniteDuration(duration, unit)) {
      p.complete(Try(message))
    }
    p.future
  }

  /**
   * Constructs a promise which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises.
   * @return a timer promise
   */
  @scala.deprecated("Use Promise.timeout(A, Duration) instead.", "2.2")
  def timeout: Future[Nothing] = {
    timeout(throw new TimeoutException("Timeout in promise"), Promise.defaultTimeout, unit = TimeUnit.MILLISECONDS)(internalContext)
  }

  /**
   * Converts an optional promise into a promise containing an
   * optional value. i.e. if the original option is None, you get
   * a Future[Option[A]] with a value of None, if the original
   * option is Some, you get a Future[Option[A]] containing
   * Some(A). Called "sequence" because its more general form
   * (see below) can operate on multi-element collections such as
   * lists.
   */
  @scala.deprecated("Provide the mapping as required.", "2.2")
  def sequence[A](in: Option[Future[A]]): Future[Option[A]] = {
    implicit val internalContext = play.core.Execution.internalContext
    in.map { p => p.map { v => Some(v) } }.getOrElse { Promise.pure(None) }
  }

  /**
   * Converts a traversable type M containing a Promise, into a Promise
   * containing type M. For example you could convert a List of Promise
   * into a Promise of a List.
   * @param in the traversable that's being converted into a promise
   * @return a Promise that's the result of the transformation
   */
  @scala.deprecated("Use scala.concurrent.Future.sequence() instead.", "2.2")
  def sequence[B, M[_]](in: M[Future[B]])(implicit toTraversableLike: M[Future[B]] => TraversableLike[Future[B], M[Future[B]]], cbf: CanBuildFrom[M[Future[B]], B, M[B]]): Future[M[B]] = {
    toTraversableLike(in).foldLeft(Promise.pure(cbf(in)))((fr, fa: Future[B]) => fr.flatMap(r => fa.map(a => r += a)(internalContext))(internalContext)).map(_.result)(internalContext)
  }

  /**
   * Converts an either containing a Promise as its Right into a Promise of an
   * Either with a plain (not-in-a-promise) value on the Right.
   * @param either A or Future[B]
   * @return a promise with Either[A,B]
   */
  @scala.deprecated("Provide the mapping as required.", "2.2")
  def sequenceEither[A, B](e: Either[A, Future[B]]): Future[Either[A, B]] = e.fold(r => Promise.pure(Left(r)), _.map(Right(_))(internalContext))

  /**
   * Converts an either containing a Promise on both Left and Right into a Promise
   * of an Either with plain (not-in-a-promise) values.
   */
  @scala.deprecated("Provide the mapping as required.", "2.2")
  def sequenceEither1[A, B](e: Either[Future[A], Future[B]]): Future[Either[A, B]] = e.fold(_.map(Left(_))(internalContext), _.map(Right(_))(internalContext))

  @scala.deprecated("Provide the mapping as required.", "2.2")
  def sequenceOption[A](o: Option[Future[A]]): Future[Option[A]] = o.map(_.map(Some(_))(internalContext)).getOrElse(Promise.pure(None))

}

