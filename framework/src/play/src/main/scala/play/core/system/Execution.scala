package play.core

import scala.concurrent.ExecutionContext
import scala.concurrent.forkjoin.{ ForkJoinWorkerThread, ForkJoinPool }
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.forkjoin.ForkJoinPool.ForkJoinWorkerThreadFactory

private[play] object Execution {

  lazy val internalContext: scala.concurrent.ExecutionContext = {

    class NamedFjpThread(fjp: ForkJoinPool) extends ForkJoinWorkerThread(fjp)

    /**
     * A named thread factory for the scala fjp as distinct from the Java one.
     */
    case class NamedFjpThreadFactory(name: String) extends ForkJoinWorkerThreadFactory {
      val threadNo = new AtomicInteger()
      val backingThreadFactory = Executors.defaultThreadFactory()

      def newThread(fjp: ForkJoinPool) = {
        val thread = new NamedFjpThread(fjp)
        thread.setName(name + "-" + threadNo.incrementAndGet())
        thread
      }
    }

    val numberOfThreads = play.api.Play.maybeApplication.map(_.configuration.getInt("internal-threadpool-size")).flatten
      .getOrElse(Runtime.getRuntime.availableProcessors)

    ExecutionContext.fromExecutorService(new ForkJoinPool(
      numberOfThreads,
      NamedFjpThreadFactory("play-internal-execution-context"),
      null,
      true))

  }

  object Implicits {

    implicit def internalContext = Execution.internalContext

  }
}
