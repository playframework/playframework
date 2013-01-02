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
    import scala.concurrent.ExecutionContext
    import java.util.concurrent.Executors
    import java.util.concurrent.ThreadFactory
    import java.util.concurrent.atomic.AtomicInteger

    implicit lazy val defaultExecutionContext: scala.concurrent.ExecutionContext = {
      val numberOfThreads = try {
        com.typesafe.config.ConfigFactory.load().getInt("iteratee-threadpool-size")
      } catch { case e: com.typesafe.config.ConfigException.Missing => Runtime.getRuntime.availableProcessors }
      val threadFactory = new ThreadFactory {
        val threadNo = new AtomicInteger()
        val backingThreadFactory = Executors.defaultThreadFactory()
        def newThread(r: Runnable) = {
          val thread = backingThreadFactory.newThread(r)
          thread.setName("iteratee-execution-context-" + threadNo.incrementAndGet())
          thread
        }
      }

      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numberOfThreads, threadFactory))
    }
  }
}
