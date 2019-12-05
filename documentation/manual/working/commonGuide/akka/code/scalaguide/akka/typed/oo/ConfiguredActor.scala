/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

// #oo-configured-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import javax.inject.Inject
import play.api.Configuration

object ConfiguredActor {
  final case class GetConfig(replyTo: ActorRef[String])

  def create(
      configuration: Configuration,
  ): Behavior[ConfiguredActor.GetConfig] = {
    Behaviors.setup { context =>
      new ConfiguredActor(context, configuration)
    }
  }
}

final class ConfiguredActor private (
    context: ActorContext[ConfiguredActor.GetConfig],
    configuration: Configuration,
) extends AbstractBehavior(context) {
  import ConfiguredActor._

  val config = configuration.get[String]("my.config")
  def onMessage(msg: GetConfig) = {
    msg.replyTo ! config
    this
  }
}
// #oo-configured-actor
