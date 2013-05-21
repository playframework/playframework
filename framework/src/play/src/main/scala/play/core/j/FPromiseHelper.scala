package play.core.j

import java.lang.{ Iterable => JIterable }
import java.util.{ List => JList, Timer, TimerTask }
import java.util.concurrent.{ Callable, TimeoutException, TimeUnit }
import play.api.libs.concurrent.{ Promise => DeprecatedPlayPromise }
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

  private val timer = new Timer()

  val defaultTimeout = DeprecatedPlayPromise.defaultTimeout

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

  // For deprecated method in play.libs.Akka
  def delayed[A](callable: Callable[A], duration: Long, unit: TimeUnit, ec: ExecutionContext): F.Promise[A] =
    delayedWith(callable.call(), duration, unit, ec)

  def delayed[A](function: F.Function0[A], duration: Long, unit: TimeUnit, ec: ExecutionContext): F.Promise[A] =
    delayedWith(function.apply(), duration, unit, ec)

  def get[A](promise: F.Promise[A], timeout: Long, unit: TimeUnit): A =
    Await.result(promise.wrapped(), Duration(timeout, unit))

  def sequence[A](promises: JIterable[F.Promise[_ <: A]], ec: ExecutionContext): F.Promise[JList[A]] = {
    val futures = JavaConverters.iterableAsScalaIterableConverter(promises).asScala.toBuffer.map((_: F.Promise[_ <: A]).wrapped)
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
    timeoutWith(Failure(new TimeoutException("Timeout in promise")), delay, unit)

  def onRedeem[A](promise: F.Promise[A], action: F.Callback[A], ec: ExecutionContext): Unit =
    promise.wrapped().onSuccess { case a => action.invoke(a) }(ec.prepare())

  def map[A, B](promise: F.Promise[A], function: F.Function[A, B], ec: ExecutionContext): F.Promise[B] =
    F.Promise.wrap[B](promise.wrapped().map((a: A) => function.apply(a))(ec.prepare()))

  def flatMap[A, B](promise: F.Promise[A], function: F.Function[A, F.Promise[B]], ec: ExecutionContext): F.Promise[B] =
    F.Promise.wrap[B](promise.wrapped().flatMap((a: A) => function.apply(a).wrapped())(ec.prepare()))

  def recover[A](promise: F.Promise[A], function: F.Function[Throwable, A], ec: ExecutionContext): F.Promise[A] =
    F.Promise.wrap[A](promise.wrapped().recover { case t => function.apply(t) }(ec.prepare()))

  def onFailure[A](promise: F.Promise[A], action: F.Callback[Throwable], ec: ExecutionContext) {
    promise.wrapped().onFailure { case t => action.invoke(t) }(ec.prepare())
  }

}
