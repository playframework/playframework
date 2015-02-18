/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import scala.language.higherKinds

import play.core._
import play.api._

import scala.concurrent.duration.{ FiniteDuration, Duration }

import java.util.concurrent.{ TimeUnit }

import scala.concurrent.{ Future, ExecutionContext, Promise => SPromise }
import scala.collection.mutable.Builder
import scala.collection._
import scala.collection.generic.CanBuildFrom
import play.core.Execution.internalContext
import scala.util.Try
import scala.util.control.NonFatal

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
  def timeout[A](message: => A, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS)(implicit ec: ExecutionContext): Future[A] = {
    val p = SPromise[A]()
    import play.api.Play.current
    Akka.system.scheduler.scheduleOnce(FiniteDuration(duration, unit)) {
      p.complete(Try(message))
    }
    p.future
  }

}

