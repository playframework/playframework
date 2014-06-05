/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import java.util.concurrent.atomic.AtomicInteger
import org.specs2.mutable.Specification
import scala.concurrent.{ Await, Future, Promise }
import scala.concurrent.duration._

object ClosableLazySpec extends Specification {

  "ClosableLazy" should {

    "create a value when first accessed" in {
      val createCount = new AtomicInteger()
      val cl = new ClosableLazy[String] {
        protected def create() = {
          createCount.incrementAndGet()
          ("hello", () => ())
        }
      }
      createCount.get must_== 0
      cl.get must_== "hello"
      createCount.get must_== 1
      cl.get must_== "hello"
      createCount.get must_== 1
      cl.close()
      createCount.get must_== 1
    }

    "call the close function when first closed" in {
      val closeCount = new AtomicInteger()

      val cl = new ClosableLazy[String] {
        protected def create() = {
          ("hat", () => closeCount.incrementAndGet())
        }
      }
      closeCount.get must_== 0
      cl.get must_== "hat"
      closeCount.get must_== 0
      cl.get must_== "hat"
      closeCount.get must_== 0
      cl.close()
      closeCount.get must_== 1
      cl.close()
      closeCount.get must_== 1

    }

    "throw an exception when accessed after being closed" in {
      val cl = new ClosableLazy[String] {
        protected def create() = ("oof", () => ())
      }
      cl.get must_== "oof"
      cl.close()
      cl.get must throwAn[IllegalStateException]
    }

  }

}
