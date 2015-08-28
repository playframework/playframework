/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import java.util.ArrayDeque
import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContextExecutor, ExecutionContext }

/**
 * Contains the default ExecutionContext used by Iteratees.
 */
object Execution {

  def defaultExecutionContext: ExecutionContext = Implicits.defaultExecutionContext

  object Implicits {
    implicit def defaultExecutionContext: ExecutionContext = Execution.trampoline
    implicit def trampoline: ExecutionContextExecutor = Execution.trampoline
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
  object trampoline extends ExecutionContextExecutor {

    /*
     * A ThreadLocal value is used to track the state of the trampoline in the current
     * thread. When a Runnable is added to the trampoline it uses the ThreadLocal to
     * see if the trampoline is already running in the thread. If so, it starts the
     * trampoline. When it finishes, it checks the ThreadLocal to see if any Runnables
     * have subsequently been scheduled for execution. It runs all the Runnables until
     * there are no more to exit, then it clears the ThreadLocal and stops running.
     *
     * ThreadLocal states:
     * - null =>
     *       - no Runnable running: trampoline is inactive in the current thread
     * - Empty =>
     *       - a Runnable is running and trampoline is active
     *       - no more Runnables are enqueued for execution after the current Runnable
     *         completes
     * - next: Runnable => 
     *       - a Runnable is running and trampoline is active
     *       - one Runnable is scheduled for execution after the current Runnable
     *         completes
     * - queue: ArrayDeque[Runnable] =>
     *       - a Runnable is running and trampoline is active
     *       - two or more Runnables are scheduled for execution after the current
     *         Runnable completes
     */
    private val local = new ThreadLocal[AnyRef]

    /** Marks an empty queue (see docs for `local`). */
    private object Empty

    def execute(runnable: Runnable): Unit = {
      local.get match {
        case null =>
          // Trampoline is inactive in this thread so start it up!
          try {
            // The queue of Runnables to run after this one
            // is initially empty.
            local.set(Empty)
            runnable.run()
            executeScheduled()
          } finally {
            // We've run all the Runnables, so show that the 
            // trampoline has been shut down.
            local.set(null)
          }
        case Empty =>
          // Add this Runnable to our empty queue
          local.set(runnable)
        case next: Runnable =>
          // Convert the single queued Runnable into an ArrayDeque
          // so we can schedule 2+ Runnables
          val runnables = new ArrayDeque[Runnable](4)
          runnables.addLast(next)
          runnables.addLast(runnable)
          local.set(runnables)
        case arrayDeque: ArrayDeque[_] =>
          // Add this Runnable to the end of the existing ArrayDeque
          val runnables = arrayDeque.asInstanceOf[ArrayDeque[Runnable]]
          runnables.addLast(runnable)
        case illegal =>
          throw new IllegalStateException(s"Unsupported trampoline ThreadLocal value: $illegal")
      }
    }

    /**
     * Run all tasks that have been scheduled in the ThreadLocal.
     */
    @tailrec
    private def executeScheduled(): Unit = {
      local.get match {
        case Empty =>
          // Nothing to run
          ()
        case next: Runnable =>
          // Mark the queue of Runnables after this one as empty
          local.set(Empty)
          // Run the only scheduled Runnable
          next.run()
          // Recurse in case more Runnables were added
          executeScheduled()
        case arrayDeque: ArrayDeque[_] =>
          val runnables = arrayDeque.asInstanceOf[ArrayDeque[Runnable]]
          // Rather than recursing, we can use a more efficient
          // while loop. The value of the ThreadLocal will stay as
          // an ArrayDeque until all the scheduled Runnables have been
          // run.
          while (!runnables.isEmpty) {
            val runnable = runnables.removeFirst()
            runnable.run()
          }
        case illegal =>
          throw new IllegalStateException(s"Unsupported trampoline ThreadLocal value: $illegal")
      }
    }

    def reportFailure(t: Throwable): Unit = t.printStackTrace()
  }

}
