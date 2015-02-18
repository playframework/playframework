/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import java.util.{ ArrayDeque, Deque }
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

/**
 * Contains the default ExecutionContext used by Iteratees.
 */
object Execution {

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
  object trampoline extends ExecutionContext {

    private val local = new ThreadLocal[Deque[Runnable]]

    def execute(runnable: Runnable): Unit = {
      local.get match {
        case null =>
          // Since there is no local queue, we need to install one and
          // start our trampolining loop.
          try {
            val installedQueue = new ArrayDeque[Runnable](4)
            installedQueue.addLast(runnable)
            local.set(installedQueue)
            while (!installedQueue.isEmpty) {
              val runnable = installedQueue.removeFirst()
              runnable.run()
            }
          } finally {
            // We've emptied the queue, so tidy up.
            local.set(null)
          }
        case existingQueue =>
          // There's already a local queue that is being executed.
          // Just stick our runnable on the end of that queue. The
          // runnable will eventually be run by the call to
          // `execute` that installed the queue.
          existingQueue.addLast(runnable)
      }
    }

    def reportFailure(t: Throwable): Unit = t.printStackTrace()
  }

}
