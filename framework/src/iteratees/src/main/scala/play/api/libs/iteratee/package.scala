package play.api.libs {

  /**
   * The Iteratee monad provides strict, safe, and functional I/O.
   */
  package object iteratee {

    type K[E, A] = Input[E] => Iteratee[E, A]

  }

}

package play.api.libs.iteratee {

  private[iteratee] object internal {
    import java.util.concurrent.{ Executors, ThreadFactory }
    import java.util.concurrent.atomic.AtomicInteger
    import play.api.libs.iteratee.Iteratee
    import scala.concurrent.{ ExecutionContext, Future }
    import scala.util.control.NonFatal

    def defaultExecutionContext: ExecutionContext = ExecutionContext.global
    
    object Implicits {
      def defaultExecutionContext: ExecutionContext = ExecutionContext.global
    }
    
    def sameThreadExecutionContext: ExecutionContext = new ExecutionContext {
      def execute(runnable: Runnable): Unit = runnable.run()
      def reportFailure(t: Throwable): Unit = t.printStackTrace()
    }
    
    def eagerFuture[A](body: => A): Future[A] = try Future.successful(body) catch { case NonFatal(e) => Future.failed(e) }
    
    def executeFuture[A](body: => Future[A])(ec: ExecutionContext): Future[A] = Future(body)(ec).flatMap(x => x)(sameThreadExecutionContext)
    
    def executeIteratee[A,E](body: => Iteratee[A,E])(ec: ExecutionContext): Iteratee[A,E] = Iteratee.flatten(Future(body)(ec))
  }
}
