/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util.concurrent.Executor

import play.utils.ExecCtxUtils
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

object ClassLoaderExecutionContext {

  /**
   * Create an ClassLoaderExecutionContext with values from the current thread.
   */
  def fromThread(delegate: ExecutionContext): ExecutionContextExecutor =
    new ClassLoaderExecutionContext(
      Thread.currentThread().getContextClassLoader(),
      delegate
    )

  /**
   * Create an ClassLoaderExecutionContext with values from the current thread.
   *
   * This method is necessary to prevent ambiguous method compile errors since ExecutionContextExecutor
   */
  def fromThread(delegate: ExecutionContextExecutor): ExecutionContextExecutor = fromThread(delegate: ExecutionContext)

  /**
   * Create an ClassLoaderExecutionContext with values from the current thread.
   */
  def fromThread(delegate: Executor): ExecutionContextExecutor =
    new ClassLoaderExecutionContext(
      Thread.currentThread().getContextClassLoader(),
      ExecutionContext.fromExecutor(delegate)
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
class ClassLoaderExecutionContext(contextClassLoader: ClassLoader, delegate: ExecutionContext)
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
      new ClassLoaderExecutionContext(contextClassLoader, delegatePrepared)
    }
  }
}
