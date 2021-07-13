/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util.concurrent.Executor

import play.utils.ExecCtxUtils
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

@deprecated("Renamed to ClassLoaderExecutionContext", "2.9.0")
object HttpExecutionContext {

  @deprecated("Use ClassLoaderExecutionContext.fromThread instead", "2.9.0")
  def fromThread(delegate: ExecutionContext): ExecutionContextExecutor =
    ClassLoaderExecutionContext.fromThread(delegate)

  @deprecated("Use ClassLoaderExecutionContext.fromThread instead", "2.9.0")
  def fromThread(delegate: ExecutionContextExecutor): ExecutionContextExecutor = fromThread(delegate: ExecutionContext)

  @deprecated("Use  instead", "2.9.0")
  def fromThread(delegate: Executor): ExecutionContextExecutor = ClassLoaderExecutionContext.fromThread(delegate)

  @deprecated("Use ClassLoaderExecutionContext.unprepared instead", "2.9.0")
  def unprepared(delegate: ExecutionContext) = ClassLoaderExecutionContext.unprepared(delegate)
}

@deprecated("Renamed to ClassLoaderExecutionContext", "2.9.0")
class HttpExecutionContext(contextClassLoader: ClassLoader, delegate: ExecutionContext)
    extends ExecutionContextExecutor {

  private val clecDelegate = new ClassLoaderExecutionContext(contextClassLoader, delegate)

  @deprecated("Use ClassLoaderExecutionContext.execute instead", "2.9.0")
  override def execute(runnable: Runnable) = clecDelegate.execute(runnable)

  @deprecated("Use ClassLoaderExecutionContext.reportFailure instead", "2.9.0")
  override def reportFailure(t: Throwable) = clecDelegate.reportFailure(t)

  @deprecated("Use ClassLoaderExecutionContext.prepare instead", "2.9.0")
  override def prepare(): ExecutionContext = clecDelegate.prepare()
}
