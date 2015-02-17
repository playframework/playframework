/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future }
import java.util.concurrent.atomic.AtomicReference

/**
 * Runs asynchronous operations in order. Operations are queued until
 * they can be run. Each item is added to a schedule, then each item
 * in the schedule is executed in order.
 *
 * {{{
 * val runQueue = new RunQueue()
 *
 * // This operation will run first. It completes when
 * // the future it returns is completed.
 * runQueue.schedule {
 *   Future { ... do some stuff ... }
 * }
 *
 * // This operation will run second. It will start running
 * // when the previous operation's futures complete.
 * runQueue.schedule {
 *   future1.flatMap(x => future2.map(y => x + y))
 * }
 *
 * // This operation will run when the second operation's
 * // future finishes. It's a simple synchronous operation.
 * runQueue.scheduleSimple {
 *   25
 * }
 * }}}
 *
 * Unlike solutions built around a standard concurrent queue, there is no
 * need to use a separate thread to read from the queue and execute each
 * operation. The RunQueue runner performs both scheduling and
 * executions of operations internally without the need for a separate
 * thread. This means the RunQueue doesn't consume any resources
 * when it isn't being used.
 *
 * No locks are held by this class, only atomic operations are used.
 */
private[play] final class RunQueue {

  import RunQueue._

  /**
   * The state of the RunQueue, either Inactive or Runnning.
   */
  private val state = new AtomicReference[Vector[Op]](null)

  /**
   * Schedule an operation to be run. The operation is considered
   * complete when the Future that it returns is completed. In other words,
   * the next operation will not be started until the future is completed.
   *
   * Successive calls to the `run` and `runSynchronous` methods use an
   * atomic value to guarantee ordering (a *happens-before* relationship).
   *
   * The operation will execute in the given ExecutionContext.
   */
  def schedule[A](body: => Future[A])(implicit ec: ExecutionContext): Unit = {
    schedule(Op(() => body.asInstanceOf[Future[Unit]], ec.prepare))
  }

  /**
   * Schedule a simple synchronous operation to be run. The operation is considered
   * complete when it finishes executing. In other words, the next operation will begin
   * execution immediately when this operation finishes execution.
   *
   * This method is equivalent to
   * {{{
   * schedule {
   *   body
   *   Future.successful(())
   * }
   * }}}
   *
   * Successive calls to the `run` and `runSynchronous` methods use an
   * atomic value to guarantee ordering (a *happens-before* relationship).
   *
   * The operation will execute in the given ExecutionContext.
   */
  def scheduleSimple(body: => Unit)(implicit ec: ExecutionContext): Unit = {
    schedule {
      body
      Future.successful(())
    }
  }

  /**
   * Schedule a reified operation for execution. If no other operations
   * are currently executing then this operation will be started immediately.
   * But if there are other operations currently running then this operation
   * be added to the pending queue of operations awaiting execution.
   *
   * This method encapsulates an atomic compare-and-set operation, therefore
   * it may be retried.
   */
  @tailrec
  private def schedule(op: Op): Unit = {
    val prevState = state.get
    val newState = prevState match {
      case null => Vector.empty
      case pending => pending :+ op
    }
    if (state.compareAndSet(prevState, newState)) {
      prevState match {
        case null =>
          // We've update the state to say that we're running an op,
          // so we need to actually start it running.
          execute(op)
        case _ =>
      }
    } else schedule(op) // Try again
  }

  private def execute(op: Op): Unit = {
    val f1: Future[Future[Unit]] = Future(op.thunk())(op.ec)
    val f2: Future[Unit] = f1.flatMap(identity)(Execution.trampoline)
    f2.onComplete(_ => opExecutionComplete())(Execution.trampoline)
  }

  /**
   * *De*schedule a reified operation for execution. If no other operations
   * are pending, then the RunQueue will enter an inactive state.
   * Otherwise, the first pending item will be scheduled for execution.
   */
  @tailrec
  private def opExecutionComplete(): Unit = {
    val prevState = state.get
    val newState = prevState match {
      case null => throw new IllegalStateException("Can't be inactive, must have a queue of pending elements")
      case pending if pending.isEmpty => null
      case pending => pending.tail
    }
    if (state.compareAndSet(prevState, newState)) {
      prevState match {
        // We have a pending operation to execute
        case pending if !pending.isEmpty => execute(pending.head)
        case _ =>
      }
    } else opExecutionComplete() // Try again
  }

}

private object RunQueue {

  /**
   * A reified operation to be executed.
   *
   * @param thunk The logic to execute.
   * @param ec The ExecutionContext to use for execution. Already prepared.
   */
  final case class Op(thunk: () => Future[Unit], ec: ExecutionContext)

}
