/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject

import org.specs2.mutable.Specification

import scala.collection.mutable
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

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
  }

}
