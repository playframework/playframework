package play.api.libs.concurrent

import play.api._
import play.api.libs.concurrent._

import akka.dispatch.{ Future, Await }
import akka.actor.ActorSystem
import scala.concurrent.util.{Duration}
import scala.concurrent.{CanAwait,ExecutionContext}

import java.util.concurrent.{ TimeUnit }

import com.typesafe.config._

/**
 * Wrapper used to transform an Akka Future to Play Promise
 */
class AkkaFuture[A](future: Future[A]) {

  /**
   * Transform this Akka future to a Play Promise.
   */
  def asPromise: Promise[A] = new AkkaPromise(future)

}

/**
 * A promise implemantation based on Akka's Future
 */
class AkkaPromise[T](future: Future[T]) extends Promise[T] {

  def isCompleted: Boolean = future.isCompleted

  def onComplete[U](func: (Either[Throwable, T]) ⇒ U)(implicit executor: ExecutionContext): Unit = future.onComplete(r => executor.execute(new Runnable() { def run() { func(r) } }))

  def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
     akka.dispatch.Await.ready(future,akka.util.Duration.fromNanos(atMost.toNanos))
     this
  }

  def result(atMost: Duration)(implicit permit: CanAwait): T = akka.dispatch.Await.result(future,akka.util.Duration.fromNanos(atMost.toNanos))


  def value: Option[Either[Throwable, T]] = future.value

}

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
  def future[T](body: => T)(implicit app: Application): Promise[T] = {
    akka.dispatch.Future(body)(system.dispatcher).asPromise
  }

}

/**
 * Plugin managing the application Akka Actor System.
 */
class AkkaPlugin(app: Application) extends Plugin {

  private var applicationSystemEnabled = false

  lazy val applicationSystem: ActorSystem = {
    applicationSystemEnabled = true
    val system = ActorSystem("application", app.configuration.underlying)
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
