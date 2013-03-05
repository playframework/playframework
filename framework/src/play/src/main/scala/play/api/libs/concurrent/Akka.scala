package play.api.libs.concurrent

import play.api._
import play.api.libs.concurrent._

import scala.concurrent.{ Future, Await }
import util.{Success, Failure, Try}
import akka.actor.ActorSystem
import concurrent.duration.{FiniteDuration, Duration}
import scala.concurrent.{CanAwait,ExecutionContext}

import akka.dispatch.{Future => AkkaFuture, Await => AkkaAwait, ExecutionContext => AkkaExecutionContext}
import akka.util.{Duration => AkkaDuration}

/**
 * Helper to access the application defined Akka Actor system.
 */
object Akka {

  /**
   * Retrieve the application Akka Actor system.
   *
   * Example:
   * {{{
   * val newActor = Akka.system.actorOf[Props[MyActor]]
   * }}}
   */
  def system(implicit app: Application) = {
    app.plugin[AkkaPlugin].map(_.applicationSystem).getOrElse {
      sys.error("Akka plugin is not registered.")
    }
  }

  /**
   * Executes a block of code asynchronously in the application Akka Actor system.
   *
   * Example:
   * {{{
   * val promiseOfResult = Akka.future {
   *    intensiveComputing()
   * }
   * }}}
   */
  def future[T](body: => T)(implicit app: Application): Future[T] = {
    Future(body)(system.dispatcher)
  }

  /**
   * Transforms akka.dispatch.Future to scala.concurrent.Future.
   *
   * Example:
   * {{{
   *  AsyncResult{
   *    Akka.asScalaFuture(someAkkaFuture)
   *  }
   * }}}
   */
  def asScalaFuture[T](akkaFuture: AkkaFuture[T]): Future[T] = wrapAkkaFuture(akkaFuture)

  implicit def wrapAkkaFuture[T](wrapped: AkkaFuture[T]): Future[T] = new Future[T] {
    def result(atMost: Duration)(implicit permit: CanAwait) = AkkaAwait.result(wrapped, atMost)
    def isCompleted = wrapped.isCompleted
    def onComplete[U](func: (Try[T]) => U)(implicit executor: ExecutionContext) {
      wrapped.onComplete { e =>
        executor.execute(new Runnable {
          def run() {
            func(eitherToTry(e))
          }
        })
      }
    }
    def value = wrapped.value.map(e => eitherToTry(e))
    def ready(atMost: Duration)(implicit permit: CanAwait) = {
      AkkaAwait.ready(wrapped, atMost)
      this
    }
  }

  implicit def wrapAkkaExecutionContext(ec: AkkaExecutionContext): ExecutionContext = new ExecutionContext {
    def reportFailure(t: Throwable) {
      ec.reportFailure(t)
    }
    def execute(runnable: Runnable) {
      ec.execute(runnable)
    }
  }

  implicit def wrapSip14ExecutionContext(ec: ExecutionContext): AkkaExecutionContext = new AkkaExecutionContext {
    def reportFailure(t: Throwable) {
      ec.reportFailure(t)
    }
    def execute(runnable: Runnable) {
      ec.execute(runnable)
    }
  }

  def eitherToTry[T](e: Either[Throwable, T]): Try[T] = {
    e.fold(t => Failure(t), t => Success(t))
  }

  implicit def akkaDurationToSip14(duration: AkkaDuration): Duration = duration match {
    case finite: akka.util.FiniteDuration => Duration(finite.length, finite.unit)
    case infinite: AkkaDuration.Infinite => Duration.Inf
  }

  implicit def sip14DurationToAkka(duration: Duration): AkkaDuration = duration match {
    case finite: FiniteDuration => AkkaDuration(finite.length, finite.unit)
    case infinite: Duration.Infinite => AkkaDuration.Inf
  }

}

/**
 * Plugin managing the application Akka Actor System.
 */
class AkkaPlugin(app: Application) extends Plugin {

  private var applicationSystemEnabled = false

  lazy val applicationSystem: ActorSystem = {
    applicationSystemEnabled = true
    val system = ActorSystem("application", app.configuration.underlying.getConfig("play"), app.classloader)
    Logger("play").info("Starting application default Akka system.")
    system
  }

  override def onStop() {
    if (applicationSystemEnabled) {
      Logger("play").info("Shutdown application default Akka system.")
      applicationSystem.shutdown()
      applicationSystem.awaitTermination()
    }
  }

}

