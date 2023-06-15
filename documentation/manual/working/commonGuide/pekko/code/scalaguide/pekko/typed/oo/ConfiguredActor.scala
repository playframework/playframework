/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.pekko.typed.oo

// #oo-configured-actor
import javax.inject.Inject

import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import play.api.Configuration

object ConfiguredActor {
  final case class GetConfig(replyTo: ActorRef[String])

  def create(
      configuration: Configuration,
  ): Behavior[ConfiguredActor.GetConfig] = {
    Behaviors.setup { context => new ConfiguredActor(context, configuration) }
  }
}

final class ConfiguredActor private (
    context: ActorContext[ConfiguredActor.GetConfig],
    configuration: Configuration,
) extends AbstractBehavior(context) {
  import ConfiguredActor._

  val config = configuration.get[String]("my.config")
  def onMessage(msg: GetConfig): ConfiguredActor = {
    msg.replyTo ! config
    this
  }
}
// #oo-configured-actor
