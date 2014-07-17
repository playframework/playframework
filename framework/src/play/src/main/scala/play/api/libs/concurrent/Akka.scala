/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import java.util.concurrent.{ TimeUnit, TimeoutException }
import javax.inject.{ Inject, Singleton }
import play.api._
import play.core.ClosableLazy
import akka.actor.ActorSystem
import scala.concurrent.duration._

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
@Singleton
class AkkaPlugin @Inject() (app: Application) extends Plugin {

  private val lazySystem = new ClosableLazy[ActorSystem] {

    protected def create() = {
      val config = app.configuration.underlying
      val name = app.configuration.getString("play.plugins.akka.actor-system").getOrElse("application")
      val system = ActorSystem(name, app.configuration.underlying, app.classloader)
      Play.logger.info(s"Starting application default Akka system: $name")

      val close: CloseFunction = { () =>
        Play.logger.info(s"Shutdown application default Akka system: $name")
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

