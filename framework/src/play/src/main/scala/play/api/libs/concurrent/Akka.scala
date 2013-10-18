/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import play.api._
import play.core.ClosableLazy
import scala.concurrent.Future
import akka.actor.ActorSystem

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

    protected type ResourceToClose = ActorSystem

    protected def create(): CreateResult = {
      val system = ActorSystem("application", app.configuration.underlying, app.classloader)
      Play.logger.info("Starting application default Akka system.")
      CreateResult(system, system)
    }

    protected def close(systemToClose: ActorSystem) = {
      Play.logger.info("Shutdown application default Akka system.")
      systemToClose.shutdown()
      systemToClose.awaitTermination()
    }
  }

  def applicationSystem: ActorSystem = lazySystem.get()

  override def onStop() {
    lazySystem.close()
  }

}

