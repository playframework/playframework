/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.internal.libs.concurrent

import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.annotation.InternalApi

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future, TimeoutException }

/**
 * INTERNAL API: provides ways to call Akka's CoordinatedShutdown.
 *
 * This should not be necessary by user code and it is an internal API subject to change without following our
 * deprecation policy.
 */
// This is public so that it can be used in Lagom without any hacks or copy-and-paste.
@InternalApi
object CoordinatedShutdownSupport {

  /**
   * Shuts down the provided `ActorSystem` asynchronously, starting from the configured phase.
   *
   * @param actorSystem the actor system to shut down
   * @param reason the reason the actor system is shutting down
   * @return a future that completes with `Done` when the actor system has fully shut down
   */
  def asyncShutdown(actorSystem: ActorSystem, reason: CoordinatedShutdown.Reason): Future[Done] = {
    // CoordinatedShutdown may be invoked many times over the same actorSystem but
    // only the first invocation runs the tasks (later invocations are noop).
    CoordinatedShutdown(actorSystem).run(reason)
  }

  /**
   * Shuts down the provided `ActorSystem` synchronously, starting from the configured phase. This method blocks until
   * the actor system has fully shut down, or the duration exceeds timeouts for all coordinated shutdown phases.
   *
   * @param actorSystem the actor system to shut down
   * @param reason      the reason the actor system is shutting down
   * @throws InterruptedException if the current thread is interrupted while waiting
   * @throws TimeoutException if after waiting for the specified time `awaitable` is still not ready
   */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  def syncShutdown(actorSystem: ActorSystem, reason: CoordinatedShutdown.Reason): Unit = {
    // The await operation should last at most the total timeout of the coordinated shutdown.
    // We're adding a few extra seconds of margin (5 sec) to make sure the coordinated shutdown
    // has enough room to complete and yet we will timeout in case something goes wrong (invalid setup,
    // failed task, bug, etc...) preventing the coordinated shutdown from completing.
    val shutdownTimeout = CoordinatedShutdown(actorSystem).totalTimeout() + Duration(5, TimeUnit.SECONDS)
    Await.result(
      asyncShutdown(actorSystem, reason),
      shutdownTimeout
    )
  }

}
