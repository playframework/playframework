/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject

import java.util.concurrent.atomic.AtomicInteger

import org.specs2.mutable.Specification

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class DefaultApplicationLifecycleSpec extends Specification {

  import scala.concurrent.ExecutionContext.Implicits.global

  "DefaultApplicationLifecycle" should {
    // This test ensure's two things
    // 1. Stop Hooks will be called in LIFO order
    // 2. Stop Hooks won't datarace, they will never run in parallel
    "stop all the hooks in the correct order" in {
      val lifecycle = new DefaultApplicationLifecycle()
      val buffer = mutable.ListBuffer[Int]()
      lifecycle.addStopHook(() => Future(buffer.append(1)))
      lifecycle.addStopHook(() => Future(buffer.append(2)))
      lifecycle.addStopHook(() => Future(buffer.append(3)))
      Await.result(lifecycle.stop(), 10.seconds)
      Await.result(lifecycle.stop(), 10.seconds)
      buffer.toList must beEqualTo(List(3, 2, 1))
    }

    "continue when a hook returns a failed future" in {
      val lifecycle = new DefaultApplicationLifecycle()
      val buffer = mutable.ListBuffer[Int]()
      lifecycle.addStopHook(() => Future(buffer.append(1)))
      lifecycle.addStopHook(() => Future.failed(new RuntimeException("Failed stop hook")))
      lifecycle.addStopHook(() => Future(buffer.append(3)))
      Await.result(lifecycle.stop(), 10.seconds)
      Await.result(lifecycle.stop(), 10.seconds)
      buffer.toList must beEqualTo(List(3, 1))
    }

    "continue when a hook throws an exception" in {
      val lifecycle = new DefaultApplicationLifecycle()
      val buffer = mutable.ListBuffer[Int]()
      lifecycle.addStopHook(() => Future(buffer.append(1)))
      lifecycle.addStopHook(() => throw new RuntimeException("Failed stop hook"))
      lifecycle.addStopHook(() => Future(buffer.append(3)))
      Await.result(lifecycle.stop(), 10.seconds)
      Await.result(lifecycle.stop(), 10.seconds)
      buffer.toList must beEqualTo(List(3, 1))
    }

    "runs stop() only once" in {
      val counter = new AtomicInteger(0)
      val lifecycle = new DefaultApplicationLifecycle()
      lifecycle.addStopHook{
        () =>
          counter.incrementAndGet()
          Future.successful(())
      }

      val f1 = lifecycle.stop()
      val f2 = lifecycle.stop()
      val f3 = lifecycle.stop()
      val f4 = lifecycle.stop()
      Await.result(Future.sequence(Seq(f1, f2, f3, f4)), 10.seconds)
      counter.get() must beEqualTo(1)

    }
  }

}
