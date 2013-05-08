package play.core

import akka.actor._

import com.typesafe.config._
import play.api.{ Logger, Play }

/**
 * provides Play's internal actor system and the corresponding actor instances
 */
private[play] object Invoker {

  private def loadActorConfig(config: Config) = {
    config.getConfig("play")
  }

  val system: ActorSystem = Play.maybeApplication.map { app =>
    ActorSystem("play", loadActorConfig(app.configuration.underlying), app.classloader)
  } getOrElse {
    Play.logger.warn("No application found at invoker init")
    ActorSystem("play", loadActorConfig(ConfigFactory.load()))
  }

  val executionContext: scala.concurrent.ExecutionContext = system.dispatcher

}
