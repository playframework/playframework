/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import scala.language.reflectiveCalls

import org.specs2.mutable._
import java.io.OutputStream
import java.util.concurrent.{ CountDownLatch, Executors, TimeUnit }
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ ExecutionContext, Promise, Future, Await }
import scala.concurrent.duration.{ Duration, SECONDS }
import scala.util.{ Failure, Success, Try }

object RunQueueSpec extends Specification with ExecutionSpecification {

  val waitTime = Duration(20, SECONDS)

  trait QueueTester {
    def schedule(body: => Future[Unit])(implicit ec: ExecutionContext): Unit
  }

  class RunQueueTester extends QueueTester {
    val rq = new RunQueue()
    def schedule(body: => Future[Unit])(implicit ec: ExecutionContext) = rq.schedule(body)
  }

  class NaiveQueueTester extends QueueTester {
    def schedule(body: => Future[Unit])(implicit ec: ExecutionContext) = Future(body)
  }

  def countOrderingErrors(runs: Int, queueTester: QueueTester)(implicit ec: ExecutionContext): Future[Int] = {
    val result = Promise[Int]()
    val runCount = new AtomicInteger(0)
    val orderingErrors = new AtomicInteger(0)

    for (i <- 0 until runs) {
      queueTester.schedule {
        val observedRunCount = runCount.getAndIncrement()

        // Introduce another Future just to make things complicated :)
        Future {
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
    }
    result.future
  }

  "RunQueue" should {

    "run code in order" in {
      import ExecutionContext.Implicits.global

      def percentageOfRunsWithOrderingErrors(runSize: Int, queueTester: QueueTester): Int = {
        val results: Seq[Future[Int]] = for (i <- 0 until 9) yield {
          countOrderingErrors(runSize, queueTester)
        }
        Await.result(Future.sequence(results), waitTime).filter(_ > 0).size * 10
      }

      // Iteratively increase the run size until we get observable errors 90% of the time
      // We want a high error rate because we want to then use the RunQueueTester
      // on the same run size and know that it is fixing up some problems. If the run size
      // is too small then the RunQueueTester probably isn't doing anything. We use
      // dynamic run sizing because the actual size that produces errors will vary
      // depending on the environment in which this test is run.
      var runSize = 8 // This usually reaches 8192 on my dev machine with 10 simultaneous queues
      var errorPercentage = 0
      while (errorPercentage < 90 && runSize < 1000000) {
        runSize = runSize << 1
        errorPercentage = percentageOfRunsWithOrderingErrors(runSize, new NaiveQueueTester())
      }
      //println(s"Got $errorPercentage% ordering errors on run size of $runSize")

      // Now show that this run length works fine with the RunQueueTester
      percentageOfRunsWithOrderingErrors(runSize, new RunQueueTester()) must_== 0
    }

    "use the ExecutionContext exactly once per scheduled item" in {
      val rq = new RunQueue()
      mustExecute(1) { implicit runEC =>
        val runFinished = Promise[Unit]()
        rq.schedule {
          runFinished.success(())
          Future.successful(())
        }
        Await.result(runFinished.future, waitTime) must_== (())
      }
    }
  }

}
