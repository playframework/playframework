package play.api.libs.iteratee

import scala.concurrent.ExecutionContext

/**
 * Contains the default ExecutionContext used by Iteratees.
 */
private[play] object Execution {

  /**
   * Executes tests on the same thread. This can be good for performance, but care needs
   * to be taken to avoid overflowing the stack.
   */
  val sameThreadExecutionContext: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable): Unit = runnable.run()
    def reportFailure(t: Throwable): Unit = t.printStackTrace()
  }

}
