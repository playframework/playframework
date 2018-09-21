/*
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package play.api.inject

import java.util.concurrent.Callable

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.mutable.Specification
import play.libs.F

class ApplicationLifecycleSpec extends Specification {
  "ApplicationLifecycle.stop" should {
    "not return a failed future when one of the stop hooks returns a null promise" in {
      val lifecycle = new DefaultApplicationLifecycle
      lifecycle.addStopHook(new Callable[F.Promise[Void]] {
        override def call(): F.Promise[Void] = null
      })

      val stopFuture = lifecycle.stop()

      Await.result(stopFuture, Duration(1, MINUTES)) must not(throwA[Throwable])
    }
  }
}
