/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import play.libs.F
import scala.concurrent.ExecutionContext

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

  def testExecution[A](f: F.Function[TestExecutionContext, A]): A = {
    _testExecution(f.apply)
  }

  def testExecution[A](f: F.Function2[TestExecutionContext, TestExecutionContext, A]): A = {
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

  def mustExecute(expectedCount: Int, c: F.Callback[ExecutionContext]): Unit = {
    _mustExecute(expectedCount)(c.invoke)
  }

  def mustExecute(expectedCount1: Int, expectedCount2: Int, c: F.Callback2[ExecutionContext, ExecutionContext]): Unit = {
    _mustExecute(expectedCount1)(ec1 => _mustExecute(expectedCount2)(ec2 => c.invoke(ec1, ec2)))
  }

  def mustExecute(expectedCount1: Int, expectedCount2: Int, expectedCount3: Int, c: F.Callback3[ExecutionContext, ExecutionContext, ExecutionContext]): Unit = {
    _mustExecute(expectedCount1)(ec1 => _mustExecute(expectedCount2)(ec2 => _mustExecute(expectedCount3)(ec3 => c.invoke(ec1, ec2, ec3))))
  }

}
