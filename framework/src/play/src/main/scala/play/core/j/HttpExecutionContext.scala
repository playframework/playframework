/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util.concurrent.Executor

import play.mvc.Http
import scala.compat.java8.FutureConverters
import scala.compat.java8.OptionConverters._
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

object HttpExecutionContext {

  /**
   * Create an HttpExecutionContext with values from the current thread.
   */
  def fromThread(delegate: ExecutionContext): ExecutionContextExecutor =
    new HttpExecutionContext(Thread.currentThread().getContextClassLoader(), Http.Context.safeCurrent().orElse(null), delegate)

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
    new HttpExecutionContext(Thread.currentThread().getContextClassLoader(), Http.Context.safeCurrent().orElse(null), FutureConverters.fromExecutor(delegate))

  /**
   * Create an ExecutionContext that will, when prepared, be created with values from that thread.
   */
  def unprepared(delegate: ExecutionContext) = new ExecutionContext {
    def execute(runnable: Runnable) = delegate.execute(runnable) // FIXME: Make calling this an error once SI-7383 is fixed
    def reportFailure(t: Throwable) = delegate.reportFailure(t)
    override def prepare(): ExecutionContext = fromThread(delegate)
  }
}

/**
 * Manages execution to ensure that the given context ClassLoader and Http.Context are set correctly
 * in the current thread. Actual execution is performed by a delegate ExecutionContext.
 */
class HttpExecutionContext(contextClassLoader: ClassLoader, delegate: ExecutionContext) extends ExecutionContextExecutor {

  var httpContext: Http.Context = null

  @deprecated("See https://www.playframework.com/documentation/latest/JavaHttpContextMigration27", "2.7.0")
  def this(contextClassLoader: ClassLoader, httpContext: Http.Context, delegate: ExecutionContext) = {
    this(contextClassLoader, delegate)
    this.httpContext = httpContext
  }

  override def execute(runnable: Runnable) = delegate.execute(new Runnable {
    def run(): Unit = {
      val thread = Thread.currentThread()
      val oldContextClassLoader = thread.getContextClassLoader()
      val oldHttpContext = Http.Context.safeCurrent().asScala
      thread.setContextClassLoader(contextClassLoader)
      Http.Context.setCurrent(httpContext)
      try {
        runnable.run()
      } finally {
        thread.setContextClassLoader(oldContextClassLoader)
        oldHttpContext.foreach(Http.Context.setCurrent)
      }
    }
  })

  override def reportFailure(t: Throwable) = delegate.reportFailure(t)

  override def prepare(): ExecutionContext = {
    val delegatePrepared = delegate.prepare()
    if (delegatePrepared eq delegate) {
      this
    } else {
      new HttpExecutionContext(contextClassLoader, httpContext, delegatePrepared)
    }
  }
}
