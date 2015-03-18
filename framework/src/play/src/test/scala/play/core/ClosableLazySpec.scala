/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import java.util.concurrent.atomic.AtomicInteger
import org.specs2.mutable.Specification
import scala.concurrent.{ Await, Future, Promise }
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

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

    "be closable before the first call to get" in {
      val closeCount = new AtomicInteger()

      val cl = new ClosableLazy[String] {
        protected def create() = {
          ("sock", () => closeCount.incrementAndGet())
        }
      }
      closeCount.get must_== 0
      cl.close()
      closeCount.get must_== 0
      cl.get must throwAn[IllegalStateException]
      closeCount.get must_== 0
      cl.close()
      closeCount.get must_== 0

    }

    "throw an exception when accessed after being closed" in {
      val cl = new ClosableLazy[String] {
        protected def create() = ("oof", () => ())
      }
      cl.get must_== "oof"
      cl.close()
      cl.get must throwAn[IllegalStateException]
    }

    "not deadlock when get is called during the close function" in {

      val getResultPromise = Promise[String]
      val test = Future {
        lazy val cl: ClosableLazy[String] = new ClosableLazy[String] {
          protected def create() = {
            ("banana", { () =>
              val getResult = Future[String] {
                cl.get()
              }
              getResultPromise.completeWith(getResult)
              Await.result(getResult, Duration(2, MINUTES))
            })
          }
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
