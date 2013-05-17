package play.api.libs.iteratee

import play.api.libs.iteratee.internal.executeFuture
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, SECONDS }

import ExecutionContext.Implicits.global

/**
 * Common functionality for iteratee tests.
 */
trait IterateeSpecification {
  self: org.specs2.mutable.SpecificationLike =>

  val waitTime = Duration(15, SECONDS)
  def await[A](f: Future[A]): A = Await.result(f, waitTime)
  def ready[A](f: Future[A]): Future[A] = Await.ready(f, waitTime)

  def mustTransformTo[E, A](in: E*)(out: A*)(e: Enumeratee[E, A]): Unit = {
    val f = Future(Enumerator(in: _*) |>>> e &>> Iteratee.getChunks[A]).flatMap[List[A]](x => x)
    await(f) must equalTo(List(out: _*))
  }

  def enumeratorChunks[E](e: Enumerator[E]): Future[List[E]] = {
    executeFuture(e |>>> Iteratee.getChunks[E])
  }

  def mustEnumerateTo[E, A](out: A*)(e: Enumerator[E]): Unit = {
    await(enumeratorChunks(e)) must equalTo(List(out: _*))
  }

}