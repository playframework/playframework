/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util.concurrent.Executor

import play.utils.ExecCtxUtils
import scala.compat.java8.FutureConverters
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

object HttpExecutionContext {

  /**
   * Create an HttpExecutionContext with values from the current thread.
   */
  def fromThread(delegate: ExecutionContext): ExecutionContextExecutor =
    new HttpExecutionContext(
      Thread.currentThread().getContextClassLoader(),
      delegate
    )

  /**
   * Create an HttpExecutionContext with values from the current thread.
   *
   * This method is necessary to prevent ambiguous method compile errors since ExecutionContextExecutor
   */
  def fromThread(delegate: ExecutionContextExecutor): ExecutionContextExecutor = fromThread(delegate: ExecutionContext)

  /**
   * Create an HttpExecutionContext with values from the current thread.
   */
  def fromThread(delegate: Executor): ExecutionContextExecutor =
    new HttpExecutionContext(
      Thread.currentThread().getContextClassLoader(),
      FutureConverters.fromExecutor(delegate)
    )

  /**
   * Create an ExecutionContext that will, when prepared, be created with values from that thread.
   */
  def unprepared(delegate: ExecutionContext) = new ExecutionContext {
    def execute(runnable: Runnable) =
      delegate.execute(runnable) // FIXME: Make calling this an error once SI-7383 is fixed
    def reportFailure(t: Throwable)          = delegate.reportFailure(t)
    override def prepare(): ExecutionContext = fromThread(delegate)
  }
}

/**
 * Manages execution to ensure that the given context ClassLoader is set correctly
 * in the current thread. Actual execution is performed by a delegate ExecutionContext.
 */
class HttpExecutionContext(contextClassLoader: ClassLoader, delegate: ExecutionContext)
    extends ExecutionContextExecutor {
  override def execute(runnable: Runnable) =
    delegate.execute(() => {
      val thread                = Thread.currentThread()
      val oldContextClassLoader = thread.getContextClassLoader()
      thread.setContextClassLoader(contextClassLoader)
      try {
        runnable.run()
      } finally {
        thread.setContextClassLoader(oldContextClassLoader)
      }
    })

  override def reportFailure(t: Throwable) = delegate.reportFailure(t)

  override def prepare(): ExecutionContext = {
    val delegatePrepared = ExecCtxUtils.prepare(delegate)
    if (delegatePrepared eq delegate) {
      this
    } else {
      new HttpExecutionContext(contextClassLoader, delegatePrepared)
    }
  }
}
