package play.core

import akka.actor._

import com.typesafe.config._
import play.api.Play

/**
 * provides Play's internal actor system and the corresponding actor instances
 */
private[play] object Invoker {

  private def loadActorConfig = {
    val config = Play.maybeApplication.map(_.configuration.underlying).getOrElse {
      Play.logger.warn("No application found at invoker init")
      ConfigFactory.load()
    }
    config.getConfig("play")
  }

  val system: ActorSystem = ActorSystem("play", loadActorConfig)

  val executionContext: scala.concurrent.ExecutionContext = system.dispatchers.defaultGlobalDispatcher

}
