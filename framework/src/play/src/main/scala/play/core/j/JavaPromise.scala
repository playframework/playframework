package play.core.j

import java.util.{ List => JList }
import play.api.libs.concurrent._
import scala.collection.JavaConverters
import play.libs.F
import java.util.concurrent.TimeoutException

import java.util.concurrent.{ TimeUnit, Callable }

import scala.concurrent.{ Future, ExecutionContext }

import play.core.Execution.internalContext

object JavaPromise {

  def akkaAsk(actor: akka.actor.ActorRef, message: Any, timeout: akka.util.Timeout): scala.concurrent.Future[AnyRef] =
    akka.pattern.Patterns.ask(actor, message, timeout)

  def akkaFuture[T](callable: java.util.concurrent.Callable[T]) = play.libs.Akka.asPromise(akka.dispatch.Futures.future(callable, play.libs.Akka.system.dispatchers.defaultGlobalDispatcher))

  def timeout[A](callable: Callable[A], duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS, ec: ExecutionContext): scala.concurrent.Future[A] =
    play.api.libs.concurrent.Promise.timeout(callable.call(), duration, unit)(ec)

  def sequence[A](promises: JList[F.Promise[_ <: A]]): Future[JList[A]] = {
    Promise.sequence(JavaConverters.asScalaBufferConverter(promises).asScala.map(_.getWrappedPromise))
      .map(az => JavaConverters.bufferAsJavaListConverter(az).asJava)(internalContext)
  }

  def timeout[A](message: A, delay: Long, unit: java.util.concurrent.TimeUnit) = {
    Promise.timeout(message, delay, unit)(internalContext)
  }

  def timeout: Future[Nothing] = Promise.timeout

  def recover[A](promise: Future[A], f: Throwable => A, ec: ExecutionContext): Future[A] = {
    promise.recover {
      case t => f(t)
    }(ec)
  }

  def pure[A](a: A) = Promise.pure(a)

  def throwing[A](throwable: Throwable) = Promise.pure[A](throw throwable)

  def functionToScalaFunction[A, B](f: F.Function[A, B]): A => B = a => f.apply(a)
  def functionToScalaPartialFunction[A, B](f: F.Function[A, B]): PartialFunction[A, B] = {
    case a => f.apply(a)
  }
  def callbackToPartialFunction[A](c: F.Callback[A]): PartialFunction[A, Unit] = {
    case a => c.invoke(a)
  }

}
