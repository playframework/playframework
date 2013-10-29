package play.api.libs.iteratee

import play.api.libs.iteratee.internal.executeFuture
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, SECONDS }
import org.specs2.mutable.SpecificationLike
import scala.util.Try

/**
 * Common functionality for iteratee tests.
 */
trait IterateeSpecification {
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

}