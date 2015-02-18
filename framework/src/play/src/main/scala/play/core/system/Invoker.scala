/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import akka.actor._
import com.typesafe.config._
import play.api.{ Logger, Play }
import scala.concurrent.ExecutionContext

/**
 * provides Play's internal actor system and the corresponding actor instances
 */
private[play] object Invoker {

  val lazySystem = new ClosableLazy[ActorSystem] {

    private val logger = Logger(this.getClass)

    protected def create() = {
      val system = Play.maybeApplication.map { app =>
        ActorSystem("play", loadActorConfig(app.configuration.underlying), app.classloader)
      } getOrElse {
        logger.warn("No application found at invoker init")
        ActorSystem("play", loadActorConfig(ConfigFactory.load()))
      }

      val close: CloseFunction = { () =>
        system.shutdown()
        system.awaitTermination()
      }

      (system, close)
    }

    private def loadActorConfig(config: Config): Config = {
      try {
        config.getConfig("play")
      } catch {
        case _: ConfigException.Missing => ConfigFactory.empty()
      }
    }

  }

  def system: ActorSystem = lazySystem.get()
  def executionContext: ExecutionContext = system.dispatcher

}
