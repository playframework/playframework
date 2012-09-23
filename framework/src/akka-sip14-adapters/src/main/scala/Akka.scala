package akka.dispatch.sip14Adapters
import scala.util._

/**
 * A promise implemantation based on Akka's Future
 */
class AkkaPromise[T](future: akka.dispatch.Future[T]) extends scala.concurrent.Future[T] {
  import scala.concurrent._
  import scala.concurrent.util.Duration

  def isCompleted: Boolean = future.isCompleted

  def onComplete[U](func: Try[T] => U)(implicit executor: ExecutionContext): Unit = future.onComplete(r => executor.execute(new Runnable() { def run() { func(r.fold(Failure(_),Success(_))) } }))

  def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
     akka.dispatch.Await.ready(future,scala.concurrent.util.Duration.fromNanos(atMost.toNanos))
     this
  }

  def result(atMost: Duration)(implicit permit: CanAwait): T = akka.dispatch.Await.result(future,scala.concurrent.util.Duration.fromNanos(atMost.toNanos))


  def value: Option[Try[T]] = future.value.map(_.fold(Failure(_),Success(_)))

}

object `package` {

 /**
  * Implicit conversion of Future to AkkaFuture, supporting the asPromise operation.
  */
 implicit def akkaToPlay[A](future: akka.dispatch.Future[A]): scala.concurrent.Future[A] = new AkkaPromise(future)


/**
 * Wrapper used to transform an Akka Future to Play Promise
 */
class AkkaFuture[A](future: akka.dispatch.Future[A]) {

  /**
   * Transform this Akka future to a Play Promise.
   */
  def asPromise: scala.concurrent.Future[A] = new AkkaPromise(future)

}



 implicit def akkaECToScala(ec:akka.dispatch.ExecutionContext):scala.concurrent.ExecutionContext = new scala.concurrent.ExecutionContext {

    def execute(runnable: Runnable): Unit = ec.execute(runnable)

    def reportFailure(t: Throwable): Unit = ec.reportFailure(t)

  }

  implicit def scalaDurationToAkka(d:scala.concurrent.util.Duration):akka.util.Duration = akka.util.Duration(d.length,d.unit)

}
