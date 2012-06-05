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


sealed trait PromiseValue[+A] {
  def isDefined = this match { case Waiting => false; case _ => true }
}

trait NotWaiting[+A] extends PromiseValue[A] {
  /**
   * Return the value or the promise, throw it if it held an exception
   */
  def get: A = this match {
    case Thrown(e) => throw e
    case Redeemed(a) => a
  }

  def fold[B](onError: Throwable => B, onSuccess: A => B): B = this match {
    case Thrown(e) => onError(e)
    case Redeemed(r) => onSuccess(r)
  }

}

case class Thrown(e: scala.Throwable) extends NotWaiting[Nothing]
case class Redeemed[+A](a: A) extends NotWaiting[A]
case object Waiting extends PromiseValue[Nothing]

class PlayRedeemable[-A](p:scala.concurrent.Promise[A]){
  def redeem(a: => A): Unit = try(p.success(a)) catch {case e => p.failure(e)}

  def throwing(t: Throwable): Unit = p.failure(t)

}

class PlayPromise[+A](fu:scala.concurrent.Future[A]){

  def onRedeem(k: A => Unit): Unit = extend1 { case Redeemed(a) => k(a) ; case  _ => }

  def extend[B](k: Function1[Promise[A], B]): Promise[B] = {
    val result = Promise[B]()
    fu.onComplete(_ => result.redeem(k(fu)))
    result.future
  }

  def extend1[B](k: Function1[NotWaiting[A], B]): Promise[B] = extend[B](p => k(p.value1))

  def value1 = await

  def await: NotWaiting[A] = await(Promise.defaultTimeout)

  def await(timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): NotWaiting[A] = {
    scala.concurrent.Await.ready(fu,scala.concurrent.util.Duration(timeout,unit))
    fu.value.get.fold(e => Thrown(e), a => Redeemed(a))
  }

  def filter(p: A => Boolean): Promise[A] = null

  def or[B](other: Promise[B]): Promise[Either[A, B]] = {
    import scala.concurrent.stm._

    val p = Promise[Either[A, B]]()
    val ref = Ref(false)
    this.extend1 { v =>
      if (!ref.single()) {
        val iRedeemed = ref.single.getAndTransform(_ => true)

        if (! iRedeemed) { v match {
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

        if (! iRedeemed) { v match {
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

  def orTimeout[B](message: => B, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Promise[Either[A, B]] = {
    or(Promise.timeout(message, duration, unit))
  }

  def orTimeout[B](message: B): Promise[Either[A, B]] = orTimeout(message, Promise.defaultTimeout)

  def orTimeout(e: Throwable): Promise[A] = orTimeout(e, Promise.defaultTimeout).map(_.fold(a => a, e => throw e))

}

trait Redeemable[-A] {
  def redeem(a: => A): Unit
  def throwing(t: Throwable): Unit
}

object PurePromise {

  def apply[A](lazyA: => A): scala.concurrent.Future[A] = (try ( scala.concurrent.Promise.successful(lazyA) ) catch {case e => scala.concurrent.Promise.failed(e)}).future

}

object Promise {

  private[concurrent] def invoke[T](t: => T): Promise[T] = akka.dispatch.Future { t }(Promise.system.dispatcher).asPromise
  
  private [concurrent] lazy val defaultTimeout = 
    Duration(system.settings.config.getMilliseconds("promise.akka.actor.typed.timeout"), TimeUnit.MILLISECONDS).toMillis 

  private [concurrent] lazy val system = ActorSystem("promise")

  def pure[A](a: => A): Promise[A] = PurePromise(a)

  def apply[A](): scala.concurrent.Promise[A] = scala.concurrent.Promise[A]()

  def timeout[A](message: A, duration: akka.util.Duration): Promise[A] = {
    timeout(message, duration.toMillis)
  }

  def timeout[A](message: => A, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Promise[A] = {
    val p = Promise[A]()
    play.core.Invoker.system.scheduler.scheduleOnce(akka.util.Duration(duration, unit))(p.redeem(message))
    p.future
  }

  def timeout: Promise[Nothing] = {
    timeout(throw new TimeoutException("Timeout in promise"), Promise.defaultTimeout, unit = TimeUnit.MILLISECONDS )
  } 

  def sequence[A](in: Option[Promise[A]]): Promise[Option[A]] = in.map { p => p.map { v => Some(v) } }.getOrElse { Promise.pure(None) }

  def sequence[B, M[_]](in: M[Promise[B]])(implicit toTraversableLike: M[Promise[B]] => TraversableLike[Promise[B], M[Promise[B]]], cbf: CanBuildFrom[M[Promise[B]], B, M[B]]): Promise[M[B]] = {
    toTraversableLike(in).foldLeft(Promise.pure(cbf(in)))((fr, fa: Promise[B]) => for (r <- fr; a <- fa) yield (r += a)).map(_.result)
  }

  def sequenceEither[A, B](e: Either[A, Promise[B]]): Promise[Either[A, B]] = e.fold(r => Promise.pure(Left(r)), _.map(Right(_)))

  def sequenceEither1[A, B](e: Either[Promise[A], Promise[B]]): Promise[Either[A, B]] = e.fold(_.map(Left(_)), _.map(Right(_)))

  def sequenceOption[A](o: Option[Promise[A]]): Promise[Option[A]] = o.map(_.map(Some(_))).getOrElse(Promise.pure(None))

}

