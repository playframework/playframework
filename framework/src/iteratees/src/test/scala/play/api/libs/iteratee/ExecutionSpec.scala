/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import scala.language.reflectiveCalls

import org.specs2.mutable._
import java.io.OutputStream
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import scala.concurrent.{ ExecutionContext, Promise, Future, Await }
import scala.concurrent.duration.{ Duration, SECONDS }
import scala.util.{ Failure, Success, Try }

object ExecutionSpec extends Specification {
  import Execution.trampoline

  val waitTime = Duration(5, SECONDS)

  "trampoline" should {

    "execute code in the same thread" in {
      val f = Future(Thread.currentThread())(trampoline)
      Await.result(f, waitTime) must equalTo(Thread.currentThread())
    }

    "not overflow the stack" in {
      def executeRecursively(ec: ExecutionContext, times: Int) {
        if (times > 0) {
          ec.execute(new Runnable {
            def run() = executeRecursively(ec, times - 1)
          })
        }
      }

      // Work out how deep to go to cause an overflow
      val overflowingExecutionContext = new ExecutionContext {
        def execute(runnable: Runnable): Unit = {
          runnable.run()
        }
        def reportFailure(t: Throwable): Unit = t.printStackTrace()
      }

      var overflowTimes = 1 << 10
      try {
        while (overflowTimes > 0) {
          executeRecursively(overflowingExecutionContext, overflowTimes)
          overflowTimes = overflowTimes << 1
        }
        sys.error("Can't get the stack to overflow")
      } catch {
        case _: StackOverflowError => ()
      }

      // Now verify that we don't overflow
      Try(executeRecursively(trampoline, overflowTimes)) must beSuccessfulTry[Unit]
    }

    "execute code in the order it was submitted" in {
      val runRecord = scala.collection.mutable.Buffer.empty[Int]
      case class TestRunnable(id: Int, children: Runnable*) extends Runnable {
        def run() = {
          runRecord += id
          for (c <- children) trampoline.execute(c)
        }
      }

      trampoline.execute(
        TestRunnable(0,
          TestRunnable(1),
          TestRunnable(2,
            TestRunnable(4,
              TestRunnable(6),
              TestRunnable(7)),
            TestRunnable(5,
              TestRunnable(8))),
          TestRunnable(3))
      )

      runRecord must equalTo(0 to 8)
    }

  }

}
