package play.api.libs.akka

import akka.dispatch.{ Future }

import play.api.libs.concurrent._
import java.util.concurrent.TimeUnit

/**
 * Defines convenient helpers to work with Akka from Play.
 */
object `package` {
  implicit def akkaToPlay[A](future: Future[A]) = new AkkaFuture(future)
}

/**
 * Wrapper used to transform an Akka Future to Play Promise
 */
class AkkaFuture[A](future: Future[A]) {
  def asPromise: Promise[A] = new AkkaPromise(future)
}

/**
 * a promise impelemantation based on Akka's Future
 */
class AkkaPromise[A](future: Future[A]) extends Promise[A] {

  /**
   * call back hook
   */
  def onRedeem(k: A => Unit) {
    future.onTimeout {
      throw new java.util.concurrent.TimeoutException("Promise timed out after " + future.timeoutInNanos / 1000000 + " milliseconds")
    }.onComplete {
      _.value.get match {
        case Left(problem) => throw problem
        case Right(result) => k(result)
      }
    }
  }

  /*
   * extend @param k 
   */
  def extend[B](k: Function1[Promise[A], B]): Promise[B] =
    new AkkaPromise[B](future.map(p => k(this)))

  /*
   * it's time to retrieve the future value
   */
  def await(timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): NotWaiting[A] =
    future.await(akka.util.Duration(timeout, unit))
      .value.get.fold(Thrown(_), Redeemed(_))

  /*
   * filtering akka based future and rewrapping the result in an AkkaPromise
   */
  def filter(p: A => Boolean): Promise[A] =
    new AkkaPromise[A](future.filter(p.asInstanceOf[(Any => Boolean)]).asInstanceOf[Future[A]])

  /*
   * mapping @param f function to AkkaPromise 
   *
   */
  def map[B](f: A => B): Promise[B] = new AkkaPromise[B](future.map(f))

  /**
   * provides a means to flatten Akka based promises
   */
  def flatMap[B](f: A => Promise[B]): Promise[B] = {
    val result = Promise[B]()
    future.map(f(_).map(result.redeem(_)))
    result
  }
}
