/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import java.util.concurrent.{ TimeUnit, TimeoutException }
import play.api._
import play.core.ClosableLazy
import scala.concurrent.Future
import akka.actor.ActorSystem
import scala.concurrent.duration._

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

}

/**
 * Plugin managing the application Akka Actor System.
 */
class AkkaPlugin(app: Application) extends Plugin {

  private val lazySystem = new ClosableLazy[ActorSystem] {

    protected def create() = {
      val system = ActorSystem("application", app.configuration.underlying, app.classloader)
      Play.logger.info("Starting application default Akka system.")

      val close: CloseFunction = { () =>
        Play.logger.info("Shutdown application default Akka system.")
        system.shutdown()

        app.configuration.getMilliseconds("play.akka.shutdown-timeout") match {
          case Some(timeout) =>
            try {
              system.awaitTermination(Duration(timeout, TimeUnit.MILLISECONDS))
            } catch {
              case te: TimeoutException =>
                // oh well.  We tried to be nice.
                Play.logger.info(s"Could not shutdown the Akka system in $timeout milliseconds.  Giving up.")
            }
          case None =>
            // wait until it is shutdown
            system.awaitTermination()
        }
      }

      (system, close)
    }

  }

  def applicationSystem: ActorSystem = lazySystem.get()

  override def onStop() {
    lazySystem.close()
  }

}

