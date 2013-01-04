package play.core

import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

private[play] object Execution {

  lazy val internalContext: scala.concurrent.ExecutionContext = {
    val numberOfThreads = play.api.Play.maybeApplication.map(_.configuration.getInt("internal-threadpool-size")).flatten.getOrElse(Runtime.getRuntime.availableProcessors)

    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numberOfThreads, NamedThreadFactory("play-internal-execution-context")))
  }
  
}
