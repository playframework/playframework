package play.api.libs.iteratee

import scala.concurrent.ExecutionContext

/**
 * Contains the default ExecutionContext used by Iteratees.
 */
private[play] object Execution {

  def defaultExecutionContext: ExecutionContext = Implicits.defaultExecutionContext

  object Implicits {
    implicit lazy val defaultExecutionContext: ExecutionContext = scala.concurrent.ExecutionContext.fromExecutor(null)
  }

  val sameThreadExecutionContext: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable): Unit = runnable.run()
    def reportFailure(t: Throwable): Unit = t.printStackTrace()
  }

}
