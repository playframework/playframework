/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import java.util.concurrent.Callable

import com.google.inject.Singleton
import play.api.Logger
import play.libs.F

import scala.concurrent.Future

/**
 * Application lifecycle register.
 *
 * This is used to hook into Play lifecycle events, specifically, when Play is stopped.
 *
 * Stop hooks are executed when the application is shutdown, in reverse from when they were registered.
 *
 * To use this, declare a dependency on ApplicationLifecycle, and then register the stop hook when the component is
 * started.
 */
trait ApplicationLifecycle {

  /**
   * Add a stop hook to be called when the application stops.
   *
   * The stop hook should redeem the returned future when it is finished shutting down.  It is acceptable to stop
   * immediately and return a successful future.
   */
  def addStopHook(hook: () => Future[Unit]): Unit

  /**
   * Add a stop hook to be called when the application stops.
   *
   * The stop hook should redeem the returned future when it is finished shutting down.  It is acceptable to stop
   * immediately and return a successful future.
   */
  def addStopHook(hook: Callable[F.Promise[Void]]): Unit = {
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    addStopHook(() => hook.call().wrapped().map(_ => ()))
  }
}

/**
 * Default implementation of the application lifecycle.
 */
@Singleton
class DefaultApplicationLifecycle extends ApplicationLifecycle {
  private val mutex = new Object()
  @volatile private var hooks = List.empty[() => Future[Unit]]

  def addStopHook(hook: () => Future[Unit]) = mutex.synchronized {
    hooks = hook :: hooks
  }

  /**
   * Call to shutdown the application.
   *
   * @return A future that will be redeemed once all hooks have executed.
   */
  def stop(): Future[Unit] = {

    // Do we care if one hook executes on another hooks redeeming thread? Hopefully not.
    import play.api.libs.iteratee.Execution.Implicits.trampoline

    hooks.foldLeft(Future.successful(())) { (future, hook) =>
      future.flatMap { _ =>
        hook().recover {
          case e => Logger.error("Error executing stop hook", e)
        }
      }
    }
  }
}
