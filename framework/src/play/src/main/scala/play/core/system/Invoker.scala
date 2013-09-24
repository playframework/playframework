/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
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

    protected type ResourceToClose = ActorSystem

    protected def create(): CreateResult = {
      val system = Play.maybeApplication.map { app =>
        ActorSystem("play", loadActorConfig(app.configuration.underlying), app.classloader)
      } getOrElse {
        Play.logger.warn("No application found at invoker init")
        ActorSystem("play", loadActorConfig(ConfigFactory.load()))
      }
      CreateResult(system, system)
    }

    private def loadActorConfig(config: Config) = {
      config.getConfig("play")
    }

    protected def close(systemToClose: ActorSystem) = {
      systemToClose.shutdown()
      systemToClose.awaitTermination()
    }
  }

  def system: ActorSystem = lazySystem.get()
  def executionContext: ExecutionContext = system.dispatcher

}
