/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import play.api.libs.iteratee.internal.executeFuture
import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration.{ Duration, SECONDS, MILLISECONDS }
import org.specs2.mutable.SpecificationLike
import scala.util.Try

/**
 * Common functionality for iteratee tests.
 */
trait IterateeSpecification extends NoConcurrentExecutionContext {
  self: org.specs2.mutable.SpecificationLike =>

  val waitTime = Duration(5, SECONDS)
  def await[A](f: Future[A]): A = Await.result(f, waitTime)
  def ready[A](f: Future[A]): Future[A] = Await.ready(f, waitTime)

  def mustTransformTo[E, A](in: E*)(out: A*)(e: Enumeratee[E, A]) = {
    val f = Future(Enumerator(in: _*) |>>> e &>> Iteratee.getChunks[A])(Execution.defaultExecutionContext).flatMap[List[A]](x => x)(Execution.defaultExecutionContext)
    Await.result(f, Duration.Inf) must equalTo(List(out: _*))
  }

  def enumeratorChunks[E](e: Enumerator[E]): Future[List[E]] = {
    executeFuture(e |>>> Iteratee.getChunks[E])(Execution.defaultExecutionContext)
  }

  def mustEnumerateTo[E, A](out: A*)(e: Enumerator[E]) = {
    Await.result(enumeratorChunks(e), Duration.Inf) must equalTo(List(out: _*))
  }

  def mustPropagateFailure[E](e: Enumerator[E]) = {
    Try(Await.result(
      e(Cont { case _ => throw new RuntimeException() }),
      Duration.Inf
    )) must beAFailedTry
  }

  /**
   * Convenience function for creating a Done Iteratee that returns the given value
   */
  def done(value: String): Iteratee[String, String] = Done[String, String](value)

  /**
   * Convenience function for an Error Iteratee that contains the given error message
   */
  def error(msg: String): Iteratee[String, String] = Error[String](msg, Input.Empty)

  /**
   * Convenience function for creating a Cont Iteratee that feeds its input to the given function
   */
  def cont(f: String => Iteratee[String, String]): Iteratee[String, String] = {
    Cont[String, String]({
      case Input.El(input: String) => f(input)
      case unrecognized => throw new IllegalArgumentException(s"Unexpected input for Cont iteratee: $unrecognized")
    })
  }

  /**
   * Convenience function for creating the given Iteratee after the given delay
   */
  def delayed(it: => Iteratee[String, String], delay: Duration = Duration(5, MILLISECONDS))(implicit ec: ExecutionContext): Iteratee[String, String] = {
    Iteratee.flatten(timeout(it, delay))
  }

  val timer = new java.util.Timer(true)
  def timeout[A](a: => A, d: Duration)(implicit e: ExecutionContext): Future[A] = {
    val p = Promise[A]()
    timer.schedule(new java.util.TimerTask {
      def run() {
        p.complete(Try(a))
      }
    }, d.toMillis)
    p.future
  }
}
