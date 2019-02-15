/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ Callable, CompletionStage, ConcurrentLinkedDeque }

import akka.Done
import javax.inject.{ Inject, Singleton }
import play.api.Logger

import scala.annotation.tailrec
import scala.compat.java8.FutureConverters
import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success, Try }

/**
 * Application lifecycle register.
 *
 * This is used to hook into Play lifecycle events, specifically, when Play is stopped. The reason Play only provides
 * lifecycle callbacks for stopping is that constructors are considered the application start callback.  This has
 * several advantages:
 *
 * - It simplifies implementation, if you want to start something, just do it in the constructor.
 * - It simplifies state, there's no transitional state where an object has been created but not started yet. Hence,
 * as long as you have a reference to something, it's safe to use it.
 * - It solves startup dependencies in a type safe manner - the order that components must be started is enforced by the
 * order that they must be instantiated due to the component graph.
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

  /**
   * Call to shutdown the application and execute the registered hooks.
   *
   * Since 2.7.0, implementations of <code>stop</code> are expected to be idempotent so invoking the method
   * several times only runs the process once.
   *
   * @return A future that will be redeemed once all hooks have executed.
   */
  @deprecated("Do not invoke stop() directly. Instead, use CoordinatedShutdown.run to stop and release your resources.", "2.7.0")
  def stop(): Future[_]

  /**
   * @return the Java version for this Application Lifecycle.
   */
  def asJava: play.inject.ApplicationLifecycle = new play.inject.DelegateApplicationLifecycle(this)
}

/**
 * Default implementation of the application lifecycle.
 */
@Singleton
class DefaultApplicationLifecycle @Inject() () extends ApplicationLifecycle {
  private val hooks = new ConcurrentLinkedDeque[() => Future[_]]()

  override def addStopHook(hook: () => Future[_]): Unit = hooks.push(hook)

  private val stopPromise: Promise[Done] = Promise()
  private val started = new AtomicBoolean(false)

  /**
   * Call to shutdown the application.
   *
   * @return A future that will be redeemed once all hooks have executed.
   */
  override def stop(): Future[_] = {

    // run the code only once and memoize the result of the invocation in a Promise.future so invoking
    // the method many times causes a single run producing the same result in all cases.
    if (started.compareAndSet(false, true)) {
      // Do we care if one hook executes on another hooks redeeming thread? Hopefully not.
      import play.core.Execution.Implicits.trampoline

      @tailrec
      def clearHooks(previous: Future[Any] = Future.successful[Any](())): Future[Any] = {
        val hook = hooks.poll()
        if (hook != null) clearHooks(previous.flatMap { _ =>
          val hookFuture = Try(hook()) match {
            case Success(f) => f
            case Failure(e) => Future.failed(e)
          }
          hookFuture.recover {
            case e => Logger.error("Error executing stop hook", e)
          }
        })
        else previous
      }

      stopPromise.completeWith(clearHooks().map(_ => Done))
    }
    stopPromise.future
  }
}
