/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.utils

import util.control.Exception._
import org.specs2.mutable.Specification
import play.utils.Threads

object ThreadsSpec extends Specification {
  "Threads" should {
    "restore the correct class loader" in {
      "if the block returns successfully" in {
        val currentCl = Thread.currentThread.getContextClassLoader
        Threads.withContextClassLoader(testClassLoader) {
          Thread.currentThread.getContextClassLoader must be equalTo testClassLoader
          "a string"
        } must be equalTo "a string"
        Thread.currentThread.getContextClassLoader must be equalTo currentCl
      }

      "if the block throws an exception" in {
        val currentCl = Thread.currentThread.getContextClassLoader
        (catching(classOf[RuntimeException]) opt Threads.withContextClassLoader(testClassLoader) {
          Thread.currentThread.getContextClassLoader must be equalTo testClassLoader
          throw new RuntimeException("Uh oh")
        }) must beNone
        Thread.currentThread.getContextClassLoader must be equalTo currentCl
      }
    }
  }
  val testClassLoader = new ClassLoader() {}
}
