/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import scala.language.higherKinds

import scala.concurrent.duration.FiniteDuration

import java.util.concurrent.TimeUnit

import scala.concurrent.{ Future, ExecutionContext, Promise => SPromise }
import scala.util.Try

/**
 * useful helper methods to create and compose Promises
 */
object Promise {

  /**
   * Constructs a Future which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises
   * @param message message to be displayed
   * @param duration duration for the scheduled promise
   * @return a scheduled promise
   */
  @deprecated("Use akka.pattern.after(duration, actorSystem.scheduler)(Future(message)) instead", since = "2.5.0")
  def timeout[A](message: => A, duration: scala.concurrent.duration.Duration)(implicit ec: ExecutionContext): Future[A] = {
    timeout(message, duration.toMillis)
  }

  /**
   * Constructs a Future which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises
   * @param message message to be displayed
   * @param duration duration for the scheduled promise
   * @return a scheduled promise
   */
  @deprecated("Use akka.pattern.after(duration, actorSystem.scheduler)(Future(message)) instead", since = "2.5.0")
  def timeout[A](message: => A, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS)(implicit ec: ExecutionContext): Future[A] = {
    val p = SPromise[A]()
    val app = play.api.Play.privateMaybeApplication.get
    app.actorSystem.scheduler.scheduleOnce(FiniteDuration(duration, unit)) {
      p.complete(Try(message))
    }
    p.future
  }

}

