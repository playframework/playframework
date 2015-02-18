/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import scala.concurrent.ExecutionContext
import org.specs2.mutable.SpecificationLike

/**
 * Common functionality for iteratee tests.
 */
trait ExecutionSpecification extends NoConcurrentExecutionContext {
  self: SpecificationLike =>

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

}
