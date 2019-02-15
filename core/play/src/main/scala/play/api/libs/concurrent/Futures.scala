/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import javax.inject.Inject

import akka.Done
import akka.actor.ActorSystem

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Future, TimeoutException }
import scala.language.implicitConversions

/**
 * This trait is used to provide non-blocking timeouts and delays on an operation that returns a Future.
 *
 * You can dependency inject the Futures as follows to create a Future that will
 * timeout after a certain period of time:
 *
 * {{{
 * class MyService @Inject()(futures: Futures, piCalculator: PiCalculator) extends Timeout {
 *   def calculateWithTimeout(timeoutDuration: FiniteDuration): Future[Int] = {
 *     futures.timeout(timeoutDuration)(piCalculator.rawCalculation())
 *   }
 * }
 * }}}
 *
 * And you can also use a delay to return data after a given period of time.
 *
 * {{{
 * class PiCalculator @Inject()(futures: Futures) {
 *   def rawCalculation(): Future[Int] = {
 *     futures.delay(300 millis) { Future.successful(42) }
 *   }
 * }
 * }}}
 *
 * You should check for timeout by using `scala.concurrent.Future.recover` or `scala.concurrent.Future.recoverWith`
 * and checking for [[scala.concurrent.TimeoutException]]:
 *
 * {{{
 * val future = myService.calculateWithTimeout(100 millis).recover {
 *   case _: TimeoutException =>
 *     -1
 * }
 * }}}
 *
 * @see [[http://docs.scala-lang.org/overviews/core/futures.html Futures and Promises]]
 */
trait Futures {

  /**
   * Creates a future which will resolve to a timeout exception if the
   * given Future has not successfully completed within timeoutDuration.
   *
   * Note that timeout is not the same as cancellation.  Even in case of timeout,
   * the given future will still complete, even though that completed value
   * is not returned.
   *
   * @tparam A the result type used in the Future.
   * @param timeoutDuration the duration after which a Future.failed(TimeoutException) should be thrown.
   * @param f a call by value Future[A]
   * @return the future that completes first, either the failed future, or the operation.
   */
  def timeout[A](timeoutDuration: FiniteDuration)(f: => Future[A]): Future[A]

  /**
   * Creates a future which will be completed after the specified duration.
   *
   * @tparam A the result type used in the Future.
   * @param duration the duration to delay the future by.
   * @param f the future to delay
   */
  def delayed[A](duration: FiniteDuration)(f: => Future[A]): Future[A]

  /**
   * Creates a delayed future that is used as a supplier to other futures.
   *
   * {{{
   * val future: Future[String] = futures.delay(1 second).map(_ => "hello world!")
   * }}}
   * @param duration
   * @return a future completed successfully after a delay of duration.
   */
  def delay(duration: FiniteDuration): Future[Done]

}

/**
 * ActorSystem based timeout.
 *
 * @param actorSystem the actor system to use.
 */
class DefaultFutures @Inject() (actorSystem: ActorSystem) extends Futures {

  override def timeout[A](timeoutDuration: FiniteDuration)(f: => Future[A]): Future[A] = {
    implicit val ec = actorSystem.dispatchers.defaultGlobalDispatcher
    val timeoutFuture = akka.pattern.after(timeoutDuration, actorSystem.scheduler) {
      val msg = s"Timeout after $timeoutDuration"
      Future.failed(new TimeoutException(msg))
    }
    Future.firstCompletedOf(Seq(f, timeoutFuture))
  }

  override def delayed[A](duration: FiniteDuration)(f: => Future[A]): Future[A] = {
    implicit val ec = actorSystem.dispatcher
    akka.pattern.after(duration, actorSystem.scheduler)(f)
  }

  override def delay(duration: FiniteDuration): Future[Done] = {
    implicit val ec = actorSystem.dispatcher
    akka.pattern.after(duration, actorSystem.scheduler)(Future.successful(akka.Done))
  }

}

/**
 * Low priority implicits to add `withTimeout` methods to [[scala.concurrent.Future]].
 *
 * You can dependency inject the ActorSystem as follows to create a Future that will
 * timeout after a certain period of time:
 *
 * {{{
 * class MyService @Inject()(piCalculator: PiCalculator)(implicit futures: Futures) {
 *
 *   def calculateWithTimeout(timeoutDuration: FiniteDuration): Future[Int] = {
 *      piCalculator.rawCalculation().withTimeout(timeoutDuration)
 *   }
 * }
 * }}}
 *
 * You should check for timeout by using `scala.concurrent.Future.recover` or `scala.concurrent.Future.recoverWith`
 * and checking for [[scala.concurrent.TimeoutException]]:
 *
 * {{{
 * val future = myService.calculateWithTimeout(100 millis).recover {
 *   case _: TimeoutException =>
 *     -1
 * }
 * }}}
 */
trait LowPriorityFuturesImplicits {

  implicit class FutureOps[T](future: Future[T]) {

    /**
     * Creates a future which will resolve to a timeout exception if the
     * given [[scala.concurrent.Future]] has not successfully completed within timeoutDuration.
     *
     * Note that timeout is not the same as cancellation.  Even in case of timeout,
     * the given future will still complete, even though that completed value
     * is not returned.
     *
     * @param timeoutDuration the duration after which a Future.failed(TimeoutException) should be thrown.
     * @param futures the implicit Futures.
     * @return the future that completes first, either the failed future, or the operation.
     */
    def withTimeout(timeoutDuration: FiniteDuration)(implicit futures: Futures): Future[T] = {
      futures.timeout(timeoutDuration)(future)
    }

    /**
     * Creates a future which will resolve to a timeout exception if the
     * given Future has not successfully completed within timeoutDuration.
     *
     * This version uses an implicit [[akka.util.Timeout]] rather than a [[scala.concurrent.duration.FiniteDuration]].
     *
     * Note that timeout is not the same as cancellation.  Even in case of timeout,
     * the given future will still complete, even though that completed value
     * is not returned.
     *
     * @param akkaTimeout the duration after which a Future.failed(TimeoutException) should be thrown.
     * @param futures the implicit Futures.
     * @return the future that completes first, either the failed future, or the operation.
     */
    def withTimeout(implicit akkaTimeout: akka.util.Timeout, futures: Futures): Future[T] = {
      futures.timeout(akkaTimeout.duration)(future)
    }

    /**
     * Creates a future which will be executed after the given delay.
     *
     * @param duration the duration after which the future should be executed.
     * @param futures the implicit Futures.
     * @return the future delayed by the specified duration.
     */
    @deprecated("Use future.withDelay(duration) or futures.delayed(duration)(future)", "2.6.6")
    def withDelay[A](duration: FiniteDuration)(future: Future[A])(implicit futures: Futures): Future[A] = {
      futures.delayed(duration)(future)
    }

    /**
     * Creates a future which will be executed after the given delay.
     *
     * @param duration the duration after which the future should be executed.
     * @param futures the implicit Futures.
     * @return the future delayed by the specified duration.
     */
    def withDelay(duration: FiniteDuration)(implicit futures: Futures): Future[T] = {
      futures.delayed(duration)(future)
    }
  }
}

object Futures extends LowPriorityFuturesImplicits {

  implicit def actorSystemToFutures(implicit actorSystem: ActorSystem): Futures = {
    new DefaultFutures(actorSystem)
  }

}
