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

trait Promise[+A] {

  def onRedeem(k: A => Unit): Unit

  def recover[AA >: A](pf: PartialFunction[Throwable, AA]): Promise[AA] = extend1 {
    case Thrown(e) if pf.isDefinedAt(e) => pf(e)
    case Thrown(e) => throw e
    case Redeemed(a) => a
  }

  def extend[B](k: Function1[Promise[A], B]): Promise[B]

  def extend1[B](k: Function1[NotWaiting[A], B]): Promise[B] = extend[B](p => k(p.value))

  def value = await

  def await: NotWaiting[A] = await(Promise.defaultTimeout)

  def await(timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): NotWaiting[A]

  def filter(p: A => Boolean): Promise[A]

  def map[B](f: A => B): Promise[B]

  def flatMap[B](f: A => Promise[B]): Promise[B]

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
    p
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

class STMPromise[A] extends Promise[A] with Redeemable[A] {
  import scala.concurrent.stm._

  val actions: Ref[List[Promise[A] => Unit]] = Ref(List())
  var redeemed: Ref[PromiseValue[A]] = Ref(Waiting)

  def extend[B](k: Function1[Promise[A], B]): Promise[B] = {
    val result = new STMPromise[B]()
    addAction { p =>
      val bOrExc = scala.util.control.Exception.allCatch[B].either(k(p))
      bOrExc.fold(e => result.throwing(e), b => result.redeem(b))
    }
    result
  }

  def filter(p: A => Boolean): Promise[A] = {
    val result = new STMPromise[A]()
    this.addAction(_.value match {
      case Redeemed(a) => if (p(a)) result.redeem(a) else result.redeem(throw new NoSuchElementException)
      case Thrown(t) => result.redeem(throw t)
    })
    result
  }

  def onRedeem(k: A => Unit): Unit = {
    addAction(p => p.value match { case Redeemed(a) => k(a); case _ => })
  }

  private def addAction(k: Promise[A] => Unit): Unit = {
    if (redeemed.single().isDefined) {
      invoke(this, k)
    } else {
      val ok: Boolean = atomic { implicit txn =>
        if (!redeemed().isDefined) { actions() = actions() :+ k; true }
        else false
      }
      if (!ok) invoke(this, k)
    }
  }

  def await(timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): NotWaiting[A] = {
    atomic { implicit txn =>
      if (redeemed() != Waiting) redeemed().asInstanceOf[NotWaiting[A]]
      else {
        retryFor(unit.toNanos(timeout), scala.actors.threadpool.TimeUnit.NANOSECONDS)
        throw new TimeoutException("Promise timed out after " + timeout + " : " + unit)
      }
    }
  }

  private def invoke[T](a: T, k: T => Unit): Unit = akka.dispatch.Future { k(a) }(Promise.system.dispatcher)

  def redeem(body: => A): Unit = {
    val result = scala.util.control.Exception.allCatch[A].either(body)
    atomic { implicit txn =>
      if (redeemed().isDefined) sys.error("already redeemed")
      redeemed() = result.fold(Thrown(_), Redeemed(_))
    }
    actions.single.swap(List()).foreach(invoke(this, _))
  }

  def throwing(t: Throwable): Unit = {
    atomic { implicit txn =>
      if (redeemed().isDefined) sys.error("already redeemed")
      redeemed() = Thrown(t)
    }
    actions.single.swap(List()).foreach(invoke(this, _))
  }

  def map[B](f: A => B): Promise[B] = {
    val result = new STMPromise[B]()
    this.addAction(p => p.value match {
      case Redeemed(a) => result.redeem(f(a))
      case Thrown(e) => result.redeem(throw e)
    })
    result
  }

  def flatMap[B](f: A => Promise[B]) = {
    val result = new STMPromise[B]()
    this.addAction(p => p.value match {
      case Redeemed(a) =>
        (try {
          f(a)
        } catch {
          case e =>
            Promise.pure[B](throw e)
        }).extend(ip => ip.value match {
          case Redeemed(a) => result.redeem(a)
          case Thrown(e) => result.redeem(throw e)

        })
      case Thrown(e) => result.redeem(throw e)
    })
    result
  }
}

object PurePromise {

  def apply[A](lazyA: => A): Promise[A] = 
    (try (akka.dispatch.Promise.successful(lazyA)(Promise.system.dispatcher))
     catch{ case e => akka.dispatch.Promise.failed(e)(Promise.system.dispatcher)}).asPromise
}

object Promise {

  private[concurrent] def invoke[T](t: => T): Promise[T] = akka.dispatch.Future { t }(Promise.system.dispatcher).asPromise
  
  private [concurrent] lazy val defaultTimeout = 
    Duration(system.settings.config.getMilliseconds("promise.akka.actor.typed.timeout"), TimeUnit.MILLISECONDS).toMillis 

  private [concurrent] lazy val system = ActorSystem("promise")

  def pure[A](a: => A): Promise[A] = PurePromise(a)

  def apply[A](): Promise[A] with Redeemable[A] = new STMPromise[A]()

  def timeout[A](message: A, duration: akka.util.Duration): Promise[A] = {
    timeout(message, duration.toMillis)
  }

  def timeout[A](message: => A, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Promise[A] = {
    val p = Promise[A]()
    play.core.Invoker.system.scheduler.scheduleOnce(akka.util.Duration(duration, unit))(p.redeem(message))
    p
  }

  def timeout: Promise[TimeoutException] = {
    timeout(new TimeoutException("Timeout in promise"), Promise.defaultTimeout)
  } 

  def sequence[A](in: Option[Promise[A]]): Promise[Option[A]] = in.map { p => p.map { v => Some(v) } }.getOrElse { Promise.pure(None) }

  def sequence[B, M[_]](in: M[Promise[B]])(implicit toTraversableLike: M[Promise[B]] => TraversableLike[Promise[B], M[Promise[B]]], cbf: CanBuildFrom[M[Promise[B]], B, M[B]]): Promise[M[B]] = {
    toTraversableLike(in).foldLeft(Promise.pure(cbf(in)))((fr, fa: Promise[B]) => for (r <- fr; a <- fa) yield (r += a)).map(_.result)
  }

  def sequenceEither[A, B](e: Either[A, Promise[B]]): Promise[Either[A, B]] = e.fold(r => Promise.pure(Left(r)), _.map(Right(_)))

  def sequenceEither1[A, B](e: Either[Promise[A], Promise[B]]): Promise[Either[A, B]] = e.fold(_.map(Left(_)), _.map(Right(_)))

  def sequenceOption[A](o: Option[Promise[A]]): Promise[Option[A]] = o.map(_.map(Some(_))).getOrElse(Promise.pure(None))

}

