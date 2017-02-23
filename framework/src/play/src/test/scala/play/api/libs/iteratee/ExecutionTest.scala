/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.iteratee

import play.libs.F
import scala.concurrent.ExecutionContext
import java.util.function.Consumer
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.BiFunction

/**
 * Common functionality for Java tests that use execution contexts.
 * This is a Java-friendly version of ExecutionSpecification using F classes.
 */
class ExecutionTest {

  private def _testExecution[A](f: TestExecutionContext => A): A = {
    val ec = TestExecutionContext()
    val result = ec.preparable(f(ec))
    result
  }

  def testExecution[A](f: Function[TestExecutionContext, A]): A = {
    _testExecution(f.apply)
  }

  def testExecution[A](f: BiFunction[TestExecutionContext, TestExecutionContext, A]): A = {
    _testExecution(ec1 => _testExecution(ec2 => f(ec1, ec2)))
  }

  def testExecution[A](f: F.Function3[TestExecutionContext, TestExecutionContext, TestExecutionContext, A]): A = {
    _testExecution(ec1 => _testExecution(ec2 => _testExecution(ec3 => f(ec1, ec2, ec3))))
  }

  private def _mustExecute[A](expectedCount: => Int)(f: ExecutionContext => A): A = {
    _testExecution { tec =>
      val result = f(tec)
      assert(tec.executionCount == expectedCount, s"Expected execution count of $expectedCount but recorded ${tec.executionCount}")
      result
    }
  }

  def mustExecute(expectedCount: Int, c: Consumer[ExecutionContext]): Unit = {
    _mustExecute(expectedCount)(c.accept)
  }

  def mustExecute(expectedCount1: Int, expectedCount2: Int, c: BiConsumer[ExecutionContext, ExecutionContext]): Unit = {
    _mustExecute(expectedCount1)(ec1 => _mustExecute(expectedCount2)(ec2 => c.accept(ec1, ec2)))
  }

}
