package play.core.j

import java.util.{ List => JList }
import play.api.libs.concurrent._
import scala.collection.JavaConverters
import play.libs.F
import java.util.concurrent.TimeoutException

import java.util.concurrent.{ TimeUnit, Callable }

import scala.concurrent.Future
import play.api.libs.concurrent.execution.defaultContext

object JavaPromise {

  import akka.dispatch.sip14Adapters._

  def akkaAsk (actor: akka.actor.ActorRef, message: Any, timeout: akka.util.Timeout): scala.concurrent.Future[AnyRef] =
    akka.pattern.Patterns.ask(actor,message,timeout)

  def akkaFuture[T](callable: java.util.concurrent.Callable[T]) = play.libs.Akka.asPromise(akka.dispatch.Futures.future(callable, play.libs.Akka.system.dispatcher))

  def timeout[A](callable: Callable[A], duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): scala.concurrent.Future[A] =
    play.api.libs.concurrent.Promise.timeout(callable.call(), duration, unit)
    

  val defaultExecutionContext = play.api.libs.concurrent.execution.defaultContext

  def sequence[A](promises: JList[F.Promise[_ <: A]]): Future[JList[A]] = {
    Promise.sequence(JavaConverters.asScalaBufferConverter(promises).asScala.map(_.getWrappedPromise))
      .map(az => JavaConverters.bufferAsJavaListConverter(az).asJava)
  }

  def timeout[A](message: A, delay: Long, unit: java.util.concurrent.TimeUnit) = {
    Promise.timeout(message, delay, unit)
  }

  def timeout: Future[Nothing] = Promise.timeout

  def recover[A](promise: Future[A], f: Throwable => Future[A]): Future[A] = {
    promise.extend1 {
      case Thrown(e) => f(e)
      case Redeemed(a) => Promise.pure(a)
    }.flatMap(p => p)
  }

  def pure[A](a: A) = Promise.pure(a)

  def throwing[A](throwable: Throwable) = Promise.pure[A](throw throwable)

}
