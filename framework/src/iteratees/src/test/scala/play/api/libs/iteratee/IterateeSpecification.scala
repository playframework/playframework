package play.api.libs.iteratee

import play.api.libs.iteratee.internal.executeFuture
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, SECONDS }

/**
 * Common functionality for iteratee tests.
 */
trait IterateeSpecification {
  self: org.specs2.mutable.SpecificationLike =>

  val waitTime = Duration(5, SECONDS)
  def await[A](f: Future[A]): A = Await.result(f, waitTime)
  def ready[A](f: Future[A]): Future[A] = Await.ready(f, waitTime)

  def testExecution[A](f: TestExecutionContext => A): A = {
    val ec = TestExecutionContext()
    val result = ec.preparable(f(ec))
    result
  }

  def testExecution[A](f: (TestExecutionContext, TestExecutionContext) => A): A = {
    testExecution(ec1 => testExecution(ec2 => f(ec1, ec2)))
  }

  def testExecution[A](f: (TestExecutionContext, TestExecutionContext, TestExecutionContext) => A): A = {
    testExecution(ec1 => testExecution(ec2 => testExecution(ec3 => f(ec1, ec2, ec3))))
  }

  def mustExecute[A](expectedCount: => Int)(f: ExecutionContext => A): A = {
    testExecution { tec =>
      val result = f(tec)
      tec.executionCount must equalTo(expectedCount)
      result
    }
  }

  def mustExecute[A](expectedCount1: Int, expectedCount2: Int)(f: (ExecutionContext, ExecutionContext) => A): A = {
    mustExecute(expectedCount1)(ec1 => mustExecute(expectedCount2)(ec2 => f(ec1, ec2)))
  }

  def mustExecute[A](expectedCount1: Int, expectedCount2: Int, expectedCount3: Int)(f: (ExecutionContext, ExecutionContext, ExecutionContext) => A): A = {
    mustExecute(expectedCount1)(ec1 => mustExecute(expectedCount2)(ec2 => mustExecute(expectedCount3)(ec3 => f(ec1, ec2, ec3))))
  }

  def mustTransformTo[E, A](in: E*)(out: A*)(e: Enumeratee[E, A]): Unit = {
    val f = Future(Enumerator(in: _*) |>>> e &>> Iteratee.getChunks[A])(Execution.defaultExecutionContext).flatMap[List[A]](x => x)(Execution.defaultExecutionContext)
    Await.result(f, Duration.Inf) must equalTo(List(out: _*))
  }

  def enumeratorChunks[E](e: Enumerator[E]): Future[List[E]] = {
    executeFuture(e |>>> Iteratee.getChunks[E])(Execution.defaultExecutionContext)
  }

  def mustEnumerateTo[E, A](out: A*)(e: Enumerator[E]): Unit = {
    Await.result(enumeratorChunks(e), Duration.Inf) must equalTo(List(out: _*))
  }

}