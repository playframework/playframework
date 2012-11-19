package play.core


private[play] object Execution {

  lazy val internalContext: scala.concurrent.ExecutionContext = {
     val numberOfThreads = play.api.Play.maybeApplication.map(_.configuration.getInt("internal-threadpool-size")).flatten.getOrElse(8)
  	 scala.concurrent.ExecutionContext.fromExecutorService(java.util.concurrent.Executors.newFixedThreadPool(numberOfThreads))
  }

}
