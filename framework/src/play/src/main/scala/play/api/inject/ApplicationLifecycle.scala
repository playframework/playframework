/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.inject

import java.util.concurrent.{ CompletionStage, Callable }

import javax.inject.Singleton
import play.api.Logger

import scala.compat.java8.FutureConverters
import scala.concurrent.Future

/**
 * Application lifecycle register.
 *
 * This is used to hook into Play lifecycle events, specifically, when Play is stopped. The reason Play only provides
 * lifecycle callbacks for stopping is that constructors are considered the application start callback.  This has
 * several advantages:
 *
 * - It simplifies implementation, if you want to start something, just do it in the constructor.
 * - It simplifies state, there's no transitional state where an object has been created but not started yet. Hence,
 *   as long as you have a reference to something, it's safe to use it.
 * - It solves startup dependencies in a type safe manner - the order that components must be started is enforced by the
 *   order that they must be instantiated due to the component graph.
 *
 * Stop hooks are executed when the application is shutdown, in reverse from when they were registered. Due to this
 * reverse ordering, a component can know that it is safe to use the components it depends on as long as it hasn't
 * received a shutdown event.
 *
 * To use this, declare a dependency on ApplicationLifecycle, and then register the stop hook when the component is
 * started. For example:
 *
 * {{{
 *   import play.api.inject.ApplicationLifecycle
 *   import javax.inject.Inject
 *
 *   class SomeDatabase @Inject() (applicationLifecycle: ApplicationLifecycle) {
 *
 *     private val connectionPool = new SomeConnectionPool()
 *     applicationLifecycle.addStopHook { () =>
 *       Future.successful(connectionPool.shutdown())
 *     }
 *
 *     ...
 *   }
 * }}}
 */
trait ApplicationLifecycle {

  /**
   * Add a stop hook to be called when the application stops.
   *
   * The stop hook should redeem the returned future when it is finished shutting down.  It is acceptable to stop
   * immediately and return a successful future.
   */
  def addStopHook(hook: () => Future[_]): Unit

  /**
   * Add a stop hook to be called when the application stops.
   *
   * The stop hook should redeem the returned future when it is finished shutting down.  It is acceptable to stop
   * immediately and return a successful future.
   */
  def addStopHook(hook: Callable[_ <: CompletionStage[_]]): Unit = {
    addStopHook(() => FutureConverters.toScala(hook.call().asInstanceOf[CompletionStage[_]]))
  }
}

/**
 * Default implementation of the application lifecycle.
 */
@Singleton
class DefaultApplicationLifecycle extends ApplicationLifecycle {
  private val mutex = new Object()
  @volatile private var hooks = List.empty[() => Future[_]]

  def addStopHook(hook: () => Future[_]) = mutex.synchronized {
    hooks = hook :: hooks
  }

  /**
   * Call to shutdown the application.
   *
   * @return A future that will be redeemed once all hooks have executed.
   */
  def stop(): Future[_] = {

    // Do we care if one hook executes on another hooks redeeming thread? Hopefully not.
    import play.api.libs.iteratee.Execution.Implicits.trampoline

    hooks.foldLeft(Future.successful[Any](())) { (future, hook) =>
      future.flatMap { _ =>
        hook().recover {
          case e => Logger.error("Error executing stop hook", e)
        }
      }
    }
  }
}
