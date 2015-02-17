/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import java.lang.{ Iterable => JIterable }
import java.util.{ List => JList, Timer, TimerTask }
import java.util.concurrent.{ Callable, TimeoutException, TimeUnit }
import play.api.libs.concurrent.{ Promise => DeprecatedPlayPromise }
import play.api.libs.iteratee.Execution
import play.libs.F
import scala.collection.JavaConverters
import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration.{ Duration, MILLISECONDS }
import scala.util.{ Failure, Success, Try }

import play.core.Execution.internalContext

/**
 * Support code for the play.libs.F.Promise class. All public methods of this
 * object are used by F.Promise to implement its methods.
 */
private[play] object FPromiseHelper {

  private val timer = new Timer(true)

  def pure[A](a: A): F.Promise[A] = F.Promise.wrap(Future.successful(a))

  def throwing[A](t: Throwable): F.Promise[A] = F.Promise.wrap(Future.failed(t))

  // For deprecated method in play.libs.Akka
  def promise[A](callable: Callable[A], ec: ExecutionContext): F.Promise[A] =
    F.Promise.wrap(Future(callable.call())(ec.prepare()))

  def promise[A](function: F.Function0[A], ec: ExecutionContext): F.Promise[A] =
    F.Promise.wrap(Future(function.apply())(ec.prepare()))

  private def delayedWith[A](f: => A, delay: Long, unit: TimeUnit, ec: ExecutionContext): F.Promise[A] = {
    val pec = ec.prepare()
    val p = Promise[A]()
    timer.schedule(new TimerTask {
      def run() { p.completeWith(Future(f)(pec)) }
    }, unit.toMillis(delay))
    F.Promise.wrap[A](p.future)
  }

  def delayed[A](callable: Callable[A], duration: Long, unit: TimeUnit, ec: ExecutionContext): F.Promise[A] =
    delayedWith(callable.call(), duration, unit, ec)

  def delayed[A](function: F.Function0[A], duration: Long, unit: TimeUnit, ec: ExecutionContext): F.Promise[A] =
    delayedWith(function.apply(), duration, unit, ec)

  def get[A](promise: F.Promise[A], timeout: Long, unit: TimeUnit): A = {
    try {
      Await.result(promise.wrapped(), Duration(timeout, unit))
    } catch {
      case ex: TimeoutException => throw new F.PromiseTimeoutException(ex.getMessage, ex)
    }
  }

  def or[A, B](left: F.Promise[A], right: F.Promise[B]): F.Promise[F.Either[A, B]] = {
    import Execution.Implicits.trampoline
    val p = Promise[F.Either[A, B]]
    left.wrapped.onComplete {
      case tryA => p.tryComplete(tryA.map(F.Either.Left[A, B](_)))
    }
    right.wrapped.onComplete {
      case tryB => p.tryComplete(tryB.map(F.Either.Right[A, B](_)))
    }
    F.Promise.wrap(p.future)
  }

  def zip[A, B](pa: F.Promise[A], pb: F.Promise[B]): F.Promise[F.Tuple[A, B]] = {
    import Execution.Implicits.trampoline
    val future = pa.wrapped.zip(pb.wrapped).map { case (a, b) => new F.Tuple(a, b) }
    F.Promise.wrap(future)
  }

  def sequence[A](promises: JIterable[F.Promise[A]], ec: ExecutionContext): F.Promise[JList[A]] = {
    val futures = JavaConverters.iterableAsScalaIterableConverter(promises).asScala.toBuffer.map((_: F.Promise[A]).wrapped)
    implicit val pec = ec.prepare() // Easiest to provide implicitly so don't need to provide other implicit arg to sequence method
    F.Promise.wrap(Future.sequence(futures).map(az => JavaConverters.bufferAsJavaListConverter(az).asJava))
  }

  private def timeoutWith[A](result: Try[A], delay: Long, unit: TimeUnit): F.Promise[A] = {
    val p = Promise[A]()
    timer.schedule(new TimerTask {
      def run() { p.complete(result) }
    }, unit.toMillis(delay))
    F.Promise.wrap[A](p.future)
  }

  def timeout[A](message: A, delay: Long, unit: TimeUnit): F.Promise[A] =
    timeoutWith(Success(message), delay, unit)

  def timeout[A](delay: Long, unit: TimeUnit): F.Promise[A] =
    timeoutWith(Failure(new F.PromiseTimeoutException("Timeout in promise")), delay, unit)

  def onRedeem[A](promise: F.Promise[A], action: F.Callback[A], ec: ExecutionContext): Unit =
    promise.wrapped().onSuccess { case a => action.invoke(a) }(ec.prepare())

  def map[A, B, T >: A](promise: F.Promise[A], function: F.Function[T, B], ec: ExecutionContext): F.Promise[B] =
    F.Promise.wrap[B](promise.wrapped().map((a: A) => function.apply(a))(ec.prepare()))

  def flatMap[A, B, T >: A](promise: F.Promise[A], function: F.Function[T, F.Promise[B]], ec: ExecutionContext): F.Promise[B] =
    F.Promise.wrap[B](promise.wrapped().flatMap((a: A) => function.apply(a).wrapped())(ec.prepare()))

  def filter[A, T >: A](promise: F.Promise[A], predicate: F.Predicate[T], ec: ExecutionContext): F.Promise[A] =
    F.Promise.wrap[A](promise.wrapped().filter(predicate.test)(ec.prepare()))

  def recover[A](promise: F.Promise[A], function: F.Function[Throwable, A], ec: ExecutionContext): F.Promise[A] =
    F.Promise.wrap[A](promise.wrapped().recover { case t => function.apply(t) }(ec.prepare()))

  def recoverWith[A](promise: F.Promise[A], function: F.Function[Throwable, F.Promise[A]], ec: ExecutionContext): F.Promise[A] =
    F.Promise.wrap[A](promise.wrapped().recoverWith { case t => function.apply(t).wrapped() }(ec.prepare()))

  def fallbackTo[A](promise: F.Promise[A], fallback: F.Promise[A]): F.Promise[A] =
    F.Promise.wrap[A](promise.wrapped.fallbackTo(fallback.wrapped))

  def onFailure[A](promise: F.Promise[A], action: F.Callback[Throwable], ec: ExecutionContext) {
    promise.wrapped().onFailure { case t => action.invoke(t) }(ec.prepare())
  }

  def transform[A, B, T >: A](promise: F.Promise[A], s: F.Function[T, B], f: F.Function[Throwable, Throwable], ec: ExecutionContext): F.Promise[B] =
    F.Promise.wrap[B](promise.wrapped.transform(s.apply, f.apply)(ec.prepare()))

  def empty[A]() = {
    Promise[A]()
  }

  def getFuture[A](promise: Promise[A]): Future[A] = promise.future

  def completeWith[T](promise: Promise[T], other: Future[T], ec: ExecutionContext): Future[Void] = {
    val p = Promise[Unit]
    other.onComplete { x =>
      p.complete(Try(promise complete x))
    }(ec)
    p.future.map(_ => null)(ec)
  }

  def tryCompleteWith[T](promise: Promise[T], other: Future[T], ec: ExecutionContext): Future[Boolean] = {
    val p = Promise[Boolean]
    other.onComplete { x =>
      p.complete(Try(promise tryComplete x))
    }(ec)
    p.future
  }
}

