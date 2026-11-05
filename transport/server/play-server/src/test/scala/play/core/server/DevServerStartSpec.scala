/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.net.URLClassLoader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

import org.specs2.mutable.Specification
import play.api.inject.DefaultApplicationLifecycle
import play.api.Configuration

class DevServerStartSpec extends Specification {

  private val classLoader = new URLClassLoader(Array.empty, getClass.getClassLoader)

  /**
   * Stops the lifecycle, and fails instead of hanging if that blocks longer than the timeout.
   */
  private def stopFailedApplication(
      lifecycle: DefaultApplicationLifecycle,
      timeout: FiniteDuration = 100.millis
  ): Unit =
    Await.result(
      Future(DevServerStart.stopFailedApplication(lifecycle, classLoader, timeout))(ExecutionContext.global),
      timeout + 10.seconds
    )

  "DevServerStart.stopFailedApplication" should {

    "run the stop hooks with the class loader of the application" in {
      val lifecycle       = new DefaultApplicationLifecycle
      val hookClassLoader = Promise[ClassLoader]()
      lifecycle.addStopHook(() =>
        Future.successful(hookClassLoader.success(Thread.currentThread.getContextClassLoader))
      )

      stopFailedApplication(lifecycle)
      hookClassLoader.future.value.map(_.get) must beSome(classLoader)
    }

    "wait for the stop hooks to complete" in {
      val lifecycle = new DefaultApplicationLifecycle
      val completed = new CountDownLatch(1)
      lifecycle.addStopHook(() =>
        Future {
          Thread.sleep(50)
          completed.countDown()
        }(ExecutionContext.global)
      )

      stopFailedApplication(lifecycle, timeout = 10.seconds)
      completed.getCount must_== 0
    }

    "not block if the future of a stop hook never completes" in {
      val lifecycle = new DefaultApplicationLifecycle
      lifecycle.addStopHook(() => Promise[Unit]().future)

      stopFailedApplication(lifecycle) must not(throwA[Throwable])
    }

    "not block if a stop hook blocks" in {
      val lifecycle = new DefaultApplicationLifecycle
      val started   = new CountDownLatch(1)
      val release   = new CountDownLatch(1)
      lifecycle.addStopHook { () =>
        started.countDown()
        release.await()
        Future.unit
      }

      try {
        stopFailedApplication(lifecycle) must not(throwA[Throwable])
        started.await(10, TimeUnit.SECONDS) must beTrue
      } finally release.countDown()
    }
  }

  "DevServerStart.failedApplicationStopTimeout" should {

    def failedApplicationStopTimeout(settings: (String, Any)*) =
      DevServerStart.failedApplicationStopTimeout(
        Configuration.from(settings.toMap).withFallback(Configuration.reference)
      )

    "be Pekko's default phase timeout by default" in {
      failedApplicationStopTimeout() must_== 5.seconds
    }

    "be the configured timeout of the service-stop phase" in {
      failedApplicationStopTimeout(
        "pekko.coordinated-shutdown.phases.service-stop.timeout" -> "30 s"
      ) must_== 30.seconds
    }

    "be the configured default phase timeout if the service-stop phase has no timeout" in {
      failedApplicationStopTimeout("pekko.coordinated-shutdown.default-phase-timeout" -> "10 s") must_== 10.seconds
    }

    "fall back to Pekko's default phase timeout if the timeout is invalid" in {
      failedApplicationStopTimeout("pekko.coordinated-shutdown.phases.service-stop.timeout" -> "invalid") must_==
        5.seconds
    }

    "fall back to Pekko's default phase timeout if the configuration doesn't contain one" in {
      DevServerStart.failedApplicationStopTimeout(Configuration.empty) must_== 5.seconds
    }
  }
}
