/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import java.util.concurrent.atomic.AtomicInteger
import org.specs2.mutable.Specification
import scala.concurrent.{ Await, Future, Promise }
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

class ClosableLazySpec extends Specification {

  "ClosableLazy" should {

    "create a value when first accessed" in {
      val createCount = new AtomicInteger()
      val cl = new ClosableLazy[String, Int] {
        protected def create() = {
          createCount.incrementAndGet()
          ("hello", () => 1)
        }
        protected def closeNotNeeded = -1
      }
      createCount.get must_== 0
      cl.get must_== "hello"
      createCount.get must_== 1
      cl.get must_== "hello"
      createCount.get must_== 1
      cl.close() must_== 1
      createCount.get must_== 1
    }

    "call the close function when first closed" in {
      val closeCount = new AtomicInteger()

      val cl = new ClosableLazy[String, Int] {
        protected def create() = {
          ("hat", () => closeCount.incrementAndGet())
        }
        protected def closeNotNeeded = -1
      }
      closeCount.get must_== 0
      cl.get must_== "hat"
      closeCount.get must_== 0
      cl.get must_== "hat"
      closeCount.get must_== 0
      cl.close() must_== 1
      closeCount.get must_== 1
      cl.close() must_== -1
      closeCount.get must_== 1

    }

    "be closable before the first call to get" in {
      val closeCount = new AtomicInteger()

      val cl = new ClosableLazy[String, Int] {
        protected def create() = {
          ("sock", () => closeCount.incrementAndGet())
        }
        protected def closeNotNeeded = -1
      }
      closeCount.get must_== 0
      cl.close() must_== -1
      closeCount.get must_== 0
      cl.get must throwAn[IllegalStateException]
      closeCount.get must_== 0
      cl.close() must_== -1
      closeCount.get must_== 0

    }

    "throw an exception when accessed after being closed" in {
      val cl = new ClosableLazy[String, Int] {
        protected def create() = ("oof", () => 1)
        protected def closeNotNeeded = -1
      }
      cl.get must_== "oof"
      cl.close() must_== 1
      cl.get must throwAn[IllegalStateException]
    }

    "not deadlock when get is called during the close function" in {

      val getResultPromise = Promise[String]
      val test = Future {
        lazy val cl: ClosableLazy[String, Unit] = new ClosableLazy[String, Unit] {
          protected def create() = {
            ("banana", { () =>
              val getResult = Future[String] {
                cl.get()
              }
              getResultPromise.completeWith(getResult)
              Await.result(getResult, Duration(2, MINUTES))
            })
          }
          protected def closeNotNeeded = ()
        }
        cl.get must_== "banana"
        cl.close() must_== (())
      }

      // Our get result should happen immediately and throw an IllegalStateException
      // because the ClosableLazy is closed. Use a long duration so this will work
      // on slow machines.
      Await.result(getResultPromise.future, Duration(1, MINUTES)) must throwAn[IllegalStateException]
    }

  }

}
