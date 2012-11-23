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
    implicit lazy val defaultExecutionContext: scala.concurrent.ExecutionContext = {
      val numberOfThreads = try {
        com.typesafe.config.ConfigFactory.load().getInt("iteratee-threadpool-size")
      } catch { case e: com.typesafe.config.ConfigException.Missing => Runtime.getRuntime.availableProcessors }
      scala.concurrent.ExecutionContext.fromExecutorService(java.util.concurrent.Executors.newFixedThreadPool(numberOfThreads))
    }
  }
}
