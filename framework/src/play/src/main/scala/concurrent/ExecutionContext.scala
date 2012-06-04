/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.concurrent



import java.util.concurrent.atomic.{ AtomicInteger }
import java.util.concurrent.{ Executors, Future => JFuture, Callable, ExecutorService, Executor }
import scala.concurrent.util.Duration
import scala.concurrent.forkjoin.{ ForkJoinPool, RecursiveTask => FJTask, RecursiveAction, ForkJoinWorkerThread }
import scala.collection.generic.CanBuildFrom
import collection._



trait ExecutionContext {
  
  /** Runs a block of code on this execution context.
   */
  def execute(runnable: Runnable): Unit
  
  /** Used internally by the framework - blocks execution for at most `atMost` time while waiting
   *  for an `awaitable` object to become ready.
   *  
   *  Clients should use `scala.concurrent.blocking` instead.
   */
  def internalBlockingCall[T](awaitable: Awaitable[T], atMost: Duration): T
  
  /** Reports that an asynchronous computation failed.
   */
  def reportFailure(t: Throwable): Unit
  
}


/** Contains factory methods for creating execution contexts.
 */
object ExecutionContext {
  
  implicit val defaultExecutionContext: ExecutionContext = {
    println("original")
    scala.concurrent.defaultExecutionContext
  }

  //import scala.concurrent.{ExecutionContext , Awaitable}
  import scala.concurrent.util.Duration

  /*implicit def defaultExecutionContext1: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable): Unit = play.api.libs.concurrent.Promise.system.dispatcher.execute(runnable)

    def reportFailure(t: Throwable): Unit = play.api.libs.concurrent.Promise.system.dispatcher.reportFailure(t)

    def internalBlockingCall[T](awaitable: Awaitable[T], atMost: Duration): T = throw new Exception()

  }
  * */
  
  /** Creates an `ExecutionContext` from the given `ExecutorService`.
   */
  def fromExecutorService(e: ExecutorService, reporter: Throwable => Unit = defaultReporter): ExecutionContext with ExecutorService =
    impl.ExecutionContextImpl.fromExecutorService(e, reporter)
  
  /** Creates an `ExecutionContext` from the given `Executor`.
   */
  def fromExecutor(e: Executor, reporter: Throwable => Unit = defaultReporter): ExecutionContext with Executor =
    impl.ExecutionContextImpl.fromExecutor(e, reporter)
  
  def defaultReporter: Throwable => Unit = {
    // re-throwing `Error`s here causes an exception handling test to fail.
    //case e: Error => throw e
    case t => t.printStackTrace()
  }
  
}


