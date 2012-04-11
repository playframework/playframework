package play.api.libs.concurrent

import play.api._
import play.api.libs.concurrent._

import akka.dispatch.{ Future, Await }
import akka.actor.ActorSystem

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
class AkkaPromise[A](future: Future[A]) extends Promise[A] {

  def onRedeem(k: A => Unit) {
    future.onComplete { _.fold(Thrown(_), k) }
  }

  def extend[B](k: Function1[Promise[A], B]): Promise[B] = {
    val p = Promise[B]()
    future.onSuccess { case a => p.redeem(k(this)) }
    future.onFailure { case e => p.redeem(k(this)) }
    p
  }

  def await(timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): NotWaiting[A] = {
    try {
      Redeemed(Await.result(future, akka.util.Duration(timeout, unit)))
    } catch {
      case e => Thrown(e)
    }
  }

  def filter(p: A => Boolean): Promise[A] = {
    new AkkaPromise[A](future.filter(p.asInstanceOf[(Any => Boolean)]).asInstanceOf[Future[A]])
  }

  def map[B](f: A => B): Promise[B] = new AkkaPromise[B](future.map(f))

  def flatMap[B](f: A => Promise[B]): Promise[B] = {
    val result = Promise[B]()
    future.onSuccess {
      case a => f(a).extend1 {
        case Redeemed(a) => result.redeem(a)
        case Thrown(e) => result.throwing(e)
      }
    }
    future.onFailure { case e => result.throwing(e) }
    result
  }

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
    val system = ActorSystem("application", Configuration.load(app.path, app.mode).underlying)
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
