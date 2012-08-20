package play.api.libs.concurrent

import play.core._
import play.api._

import akka.actor._
import akka.util.Duration
import akka.actor.Actor._

import java.util.concurrent.{ TimeUnit }

import scala.collection.mutable.Builder
import scala.collection._
import scala.collection.generic.CanBuildFrom
import java.util.concurrent.TimeoutException

import play.api.libs.concurrent.execution.defaultContext

/**
 * The state of a promise; it's waiting, contains a value, or contains an exception.
 */
sealed trait PromiseValue[+A] {

  /**
   * true if the promise has either a value or an exception.
   */
  def isDefined = this match { case Waiting => false; case _ => true }
}

/**
 * A promise state that contains either a value or an exception.
 */
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
case class Thrown(e: scala.Throwable) extends NotWaiting[Nothing]

/**
 * A promise state containing a non-exception value.
 */
case class Redeemed[+A](a: A) extends NotWaiting[A]

/**
 * A promise state indicating it has not been completed yet.
 */
case object Waiting extends PromiseValue[Nothing]

class PlayRedeemable[-A](p: scala.concurrent.Promise[A]) extends Redeemable[A] {
  def redeem(a: => A): Unit = try (p.success(a)) catch { case e => p.failure(e) }

  def throwing(t: Throwable): Unit = p.failure(t)

}

/**
 * A promised value that wraps around scala.concurent.Future.
 * The value of the promise is to be computed asynchronously.
 * The promise will be completed either with the expected value type or with
 * an exception.
 */
class PlayPromise[+A](fu: scala.concurrent.Future[A]) {

  /**
   * Registers a callback to be invoked when (and if) the promise
   * is completed with a non-exception value. The callback may
   * run in another thread.
   * @param k the callback
   */
  def onRedeem(k: A => Unit): Unit = extend1 { case Redeemed(a) => k(a); case _ => }

  /**
   * Creates a new Promise[B], by running the function k on this promise
   * after it is redeemed with a value or exception. The function may
   * run in another thread. extend() is similar to map() but can modify
   * an exception contained in the promise, not only a successful value.
   * @param k function to be executed on this promise
   */
  def extend[B](k: Function1[Promise[A], B]): Promise[B] = {
    val result = Promise[B]()
    fu.onComplete(_ => result.redeem(k(fu)))
    result.future
  }

  /**
   * Like extend() but gives your function the promise's value rather
   * than the promise itself.
   * @param k function to be executed on this promise
   */
  def extend1[B](k: Function1[NotWaiting[A], B]): Promise[B] = extend[B](p => k(p.value.get.fold(Thrown(_), Redeemed(_))))

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
    scala.concurrent.Await.ready(fu, scala.concurrent.util.Duration(timeout, unit))
    fu.value.get.fold(e => Thrown(e), a => Redeemed(a))
  }

  /**
   * Computes a new Promise which will be completed with NoSuchElementException
   * if the value of this promise does not match the given predicate.
   * The predicate may be run in another thread.
   * @param p predicate
   */

  def filter(p: A => Boolean): Promise[A] = null

  /**
   * Returns a Promise representing the first-completed of this promise
   * or another promise. The resulting Promise contains either
   * Left(resultOfThisPromise), Right(resultOfOtherPromise), or
   * an exception which could be from either of the two promises.
   * @param other promise to be composed with
   */
  def or[B](other: Promise[B]): Promise[Either[A, B]] = {
    import scala.concurrent.stm._

    val p = Promise[Either[A, B]]()
    val ref = Ref(false)
    this.extend1 { v =>
      if (!ref.single()) {
        val iRedeemed = ref.single.getAndTransform(_ => true)

        if (!iRedeemed) {
          v match {
            case Redeemed(a) =>
              p.redeem(Left(a))
            case Thrown(e) =>
              p.throwing(e)
          }
        }
      }
    }
    other.extend1 { v =>
      if (!ref.single()) {
        val iRedeemed = ref.single.getAndTransform(_ => true)

        if (!iRedeemed) {
          v match {
            case Redeemed(a) =>
              p.redeem(Right(a))
            case Thrown(e) =>
              p.throwing(e)
          }
        }
      }
    }
    p.future
  }

  /**
   * Creates a timer promise with the given message.  Message a is shown if the timer promise
   * redeems before the current promise
   * @param message message to be displayed if the timer promise redeems first
   * @param duration duration
   * @param unti time unit
   * @return either the timer message or the current promise
   */
  def orTimeout[B](message: => B, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Promise[Either[A, B]] = {
    or(Promise.timeout(message, duration, unit))
  }

  /**
   * Creates a timer promise with the given message (using the deafult Promise timeout).
   * Message a is shown if the timer promise redeems before the current promise
   * @param message message to be displayed if the timer promise redeems first
   * @param duration duration
   * @param unti time unit
   * @return either the timer message or the current promise
   */

  def orTimeout[B](message: B): Promise[Either[A, B]] = orTimeout(message, Promise.defaultTimeout)

  /**
   * Creates a timer promise with  Throwable e (using the deafult Promise timeout).
   * Exception e is shown if timer promise redeemds first
   * @param e exception to be thrown
   * @return a Promise which may throw an exception
   */
  def orTimeout(e: Throwable): Promise[A] = orTimeout(e, Promise.defaultTimeout).map(_.fold(a => a, e => throw e))

}

/**
 * A redeemable can be completed exactly once with either a value of type A or an exception.
 */
trait Redeemable[-A] {

  /**
   * Complete the redeemable with a value.
   * May only be called one time, and may not be called if throwing() was also called.
   * If evaluating the value throws an exception, equivalent to calling throwing() on that exception.
   */
  def redeem(a: => A): Unit

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
object PurePromise {

  /**
   * factory method for a pure promise
   */
  def apply[A](lazyA: => A): scala.concurrent.Future[A] = (try (scala.concurrent.Promise.successful(lazyA)) catch { case e => scala.concurrent.Promise.failed(e) }).future

}

/**
 * useful helper methods to create and compose Promises
 */
object Promise {

  private var underlyingSystem: Option[ActorSystem] = Some(ActorSystem("promise"))

  private[concurrent] def invoke[T](t: => T): Promise[T] = akka.dispatch.Future { t }(Promise.system.dispatcher).asPromise

  private[concurrent] lazy val defaultTimeout =
    Duration(system.settings.config.getMilliseconds("promise.akka.actor.typed.timeout"), TimeUnit.MILLISECONDS).toMillis

  /**
   * actor system for Promises
   */
  private[concurrent] def system = underlyingSystem.getOrElse {
    val a = ActorSystem("promise")
    underlyingSystem = Some(a)
    a
  }

  /**
   * resets the underlying promise Actor System and clears Java actor references
   */
  def resetSystem(): Unit = {
    underlyingSystem.filter(_.isTerminated == false).map { s =>
      s.shutdown()
      s.awaitTermination()
    }.getOrElse(play.api.Logger.debug("trying to reset Promise actor system that was not started yet"))
    play.libs.F.Promise.resetActors()
    underlyingSystem = None
  }

  /**
   * Synonym for PurePromise.apply
   */
  def pure[A](a: => A): Promise[A] = PurePromise(a)

  /**
   * Constructs a new redeemable Promise which has not been redeemed yet.
   */
  def apply[A](): scala.concurrent.Promise[A] = scala.concurrent.Promise[A]()

  /**
   * Constructs a promise which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises
   * @param message message to be displayed
   * @param duration duration for the timer promise
   * @return a timer promise
   */
  def timeout[A](message: A, duration: akka.util.Duration): Promise[A] = {
    timeout(message, duration.toMillis)
  }

  /**
   * Constructs a promise which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises
   * @param message message to be displayed
   * @param duration duration for the timer promise
   * @return a timer promise
   */
  def timeout[A](message: => A, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Promise[A] = {
    val p = Promise[A]()
    play.core.Invoker.system.scheduler.scheduleOnce(akka.util.Duration(duration, unit))(p.redeem(message))
    p.future
  }

  /**
   * Constructs a promise which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises.
   * @return a timer promise
   */
  def timeout: Promise[Nothing] = {
    timeout(throw new TimeoutException("Timeout in promise"), Promise.defaultTimeout, unit = TimeUnit.MILLISECONDS)
  }

  /**
   * Converts an optional promise into a promise containing an
   * optional value. i.e. if the original option is None, you get
   * a Promise[Option[A]] with a value of None, if the original
   * option is Some, you get a Promise[Option[A]] containing
   * Some(A). Called "sequence" because its more general form
   * (see below) can operate on multi-element collections such as
   * lists.
   */
  def sequence[A](in: Option[Promise[A]]): Promise[Option[A]] = in.map { p => p.map { v => Some(v) } }.getOrElse { Promise.pure(None) }

  /**
   * Converts a traversable type M containing a Promise, into a Promise
   * containing type M. For example you could convert a List of Promise
   * into a Promise of a List.
   * @param in the traversable that's being converted into a promise
   * @return a Promise that's the result of the transformation
   */
  def sequence[B, M[_]](in: M[Promise[B]])(implicit toTraversableLike: M[Promise[B]] => TraversableLike[Promise[B], M[Promise[B]]], cbf: CanBuildFrom[M[Promise[B]], B, M[B]]): Promise[M[B]] = {
    toTraversableLike(in).foldLeft(Promise.pure(cbf(in)))((fr, fa: Promise[B]) => for (r <- fr; a <- fa) yield (r += a)).map(_.result)
  }

  /**
   * Converts an either containing a Promise as its Right into a Promise of an
   * Either with a plain (not-in-a-promise) value on the Right.
   * @param either A or Promise[B]
   * @return a promise with Either[A,B]
   */
  def sequenceEither[A, B](e: Either[A, Promise[B]]): Promise[Either[A, B]] = e.fold(r => Promise.pure(Left(r)), _.map(Right(_)))

  /**
   * Converts an either containing a Promise on both Left and Right into a Promise
   * of an Either with plain (not-in-a-promise) values.
   */
  def sequenceEither1[A, B](e: Either[Promise[A], Promise[B]]): Promise[Either[A, B]] = e.fold(_.map(Left(_)), _.map(Right(_)))

  @deprecated("use sequence instead", "2.1")
  def sequenceOption[A](o: Option[Promise[A]]): Promise[Option[A]] = o.map(_.map(Some(_))).getOrElse(Promise.pure(None))

}

