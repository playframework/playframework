package play.api.libs.concurrent

import play.api._
import play.api.libs.concurrent._

import scala.concurrent.{ Future, Await }
import scala.util.Try
import akka.actor.ActorSystem
import scala.concurrent.duration.{Duration}
import scala.concurrent.{CanAwait,ExecutionContext}

import java.util.concurrent.{ TimeUnit }

import com.typesafe.config._



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

}

/**
 * Plugin managing the application Akka Actor System.
 */
class AkkaPlugin(app: Application) extends Plugin {

  private var applicationSystemEnabled = false

  lazy val applicationSystem: ActorSystem = {
    applicationSystemEnabled = true
    val system = ActorSystem("application", app.configuration.underlying, app.classloader)
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

