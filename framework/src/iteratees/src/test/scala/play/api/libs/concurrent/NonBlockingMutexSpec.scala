/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import scala.language.reflectiveCalls

import org.specs2.mutable._
import java.io.OutputStream
import java.util.concurrent.{ CountDownLatch, Executors, TimeUnit }
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ ExecutionContext, Promise, Future, Await }
import scala.concurrent.duration.{ Duration, SECONDS }
import scala.util.{ Failure, Success, Try }

object NonBlockingMutexSpec extends Specification {

  val waitTime = Duration(2, SECONDS)

  trait Tester {
    def run(body: => Unit): Unit
  }

  class MutexTester extends Tester {
    val mutex = new NonBlockingMutex()
    def run(body: => Unit) = mutex.exclusive(body)
  }

  class NaiveTester extends Tester {
    def run(body: => Unit) = body
  }

  def countOrderingErrors(runs: Int, tester: Tester)(implicit ec: ExecutionContext): Future[Int] = {
    val result = Promise[Int]()
    val runCount = new AtomicInteger(0)
    val orderingErrors = new AtomicInteger(0)

    for (i <- 0 until runs) {
      tester.run {
        val observedRunCount = runCount.getAndIncrement()

        // We see observedRunCount != i then this task was run out of order
        if (observedRunCount != i) {
          orderingErrors.incrementAndGet() // Record the error
        }
        // If this is the last task, complete our result promise
        if ((observedRunCount + 1) >= runs) {
          result.success(orderingErrors.get)
        }
      }
    }
    result.future
  }

  "NonBlockingMutex" should {

    "run a single operation" in {
      val p = Promise[Int]()
      val mutex = new NonBlockingMutex()
      mutex.exclusive { p.success(1) }
      Await.result(p.future, waitTime) must_== (1)
    }

    "run two operations" in {
      val p1 = Promise[Unit]()
      val p2 = Promise[Unit]()
      val mutex = new NonBlockingMutex()
      mutex.exclusive { p1.success(()) }
      mutex.exclusive { p2.success(()) }
      Await.result(p1.future, waitTime) must_== (())
      Await.result(p2.future, waitTime) must_== (())
    }

    "run code in order" in {
      import ExecutionContext.Implicits.global

      def percentageOfRunsWithOrderingErrors(runSize: Int, tester: Tester): Int = {
        val results: Seq[Future[Int]] = for (i <- 0 until 9) yield {
          countOrderingErrors(runSize, tester)
        }
        Await.result(Future.sequence(results), waitTime).filter(_ > 0).size * 10
      }

      // Iteratively increase the run size until we get observable errors 90% of the time
      // We want a high error rate because we want to then use the MutexTester
      // on the same run size and know that it is fixing up some problems. If the run size
      // is too small then the MutexTester probably isn't doing anything. We use
      // dynamic run sizing because the actual size that produces errors will vary
      // depending on the environment in which this test is run.
      var runSize = 8 // This usually reaches 8192 on my dev machine with 10 simultaneous queues
      var errorPercentage = 0
      while (errorPercentage < 90 && runSize < 1000000) {
        runSize = runSize << 1
        errorPercentage = percentageOfRunsWithOrderingErrors(runSize, new NaiveTester())
      }
      //println(s"Got $errorPercentage% ordering errors on run size of $runSize")

      // Now show that this run length works fine with the MutexTester
      percentageOfRunsWithOrderingErrors(runSize, new MutexTester()) must_== 0
    }

  }

}
