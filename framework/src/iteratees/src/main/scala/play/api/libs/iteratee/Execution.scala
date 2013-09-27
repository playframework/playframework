package play.api.libs.iteratee

import java.util.{ ArrayDeque, Deque }
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

/**
 * Contains the default ExecutionContext used by Iteratees.
 */
private[play] object Execution {

  def defaultExecutionContext: ExecutionContext = Implicits.defaultExecutionContext

  object Implicits {
    implicit def defaultExecutionContext: ExecutionContext = Execution.trampoline
    implicit def trampoline: ExecutionContext = Execution.trampoline
  }

  /**
   * Executes in the current thread. Uses a thread local trampoline to make sure the stack
   * doesn't overflow. Since this ExecutionContext executes on the current thread, it should
   * only be used to run small bits of fast-running code. We use it here to run the internal
   * iteratee code.
   *
   * Blocking should be strictly avoided as it could hog the current thread.
   * Also, since we're running on a single thread, blocking code risks deadlock.
   */
  val trampoline: ExecutionContext = new ExecutionContext {

    private val local = new ThreadLocal[Deque[Runnable]]

    def execute(runnable: Runnable): Unit = {
      var queue = local.get()
      if (queue == null) {
        // Since there is no local queue, we need to install one and
        // start our trampolining loop.
        try {
          queue = new ArrayDeque(4)
          queue.addLast(runnable)
          local.set(queue)
          while (!queue.isEmpty) {
            val runnable = queue.removeFirst()
            runnable.run()
          }
        } finally {
          // We've emptied the queue, so tidy up.
          local.set(null)
        }
      } else {
        // There's already a local queue that is being executed.
        // Just stick our runnable on the end of that queue.
        queue.addLast(runnable)
      }
    }

    def reportFailure(t: Throwable): Unit = t.printStackTrace()
  }

  /**
   * Executes in the current thread. Calls Runnables directly so it is possible for the
   * stack to overflow. To avoid overflow the `trampoline`
   * can be used instead.
   *
   * Blocking should be strictly avoided as it could hog the current thread.
   * Also, since we're running on a single thread, blocking code risks deadlock.
   */
  val overflowingExecutionContext: ExecutionContext = new ExecutionContext {

    def execute(runnable: Runnable): Unit = {
      runnable.run()
    }

    def reportFailure(t: Throwable): Unit = t.printStackTrace()

  }

}
