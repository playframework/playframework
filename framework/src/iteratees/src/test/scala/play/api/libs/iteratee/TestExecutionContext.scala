package play.api.libs.iteratee

import scala.concurrent.ExecutionContext

object TestExecutionContext {
  
  /**
   * Create a `TestExecutionContext` that delegates to the iteratee package's default `ExecutionContext`.
   */
  def apply(): TestExecutionContext = new TestExecutionContext(internal.defaultExecutionContext)

}

/**
 * An `ExecutionContext` that counts its executions.
 * 
 * @param delegate The underlying `ExecutionContext` to delegate execution to.
 */
class TestExecutionContext(delegate: ExecutionContext) extends ExecutionContext {
  val count = new java.util.concurrent.atomic.AtomicInteger()
  def execute(runnable: Runnable): Unit = {
    count.getAndIncrement()
    delegate.prepare().execute(runnable)
  }
  
  def reportFailure(t: Throwable): Unit = delegate.reportFailure(t)
  
  override def prepare(): ExecutionContext = this
  
  def executionCount: Int = count.get()
  
}