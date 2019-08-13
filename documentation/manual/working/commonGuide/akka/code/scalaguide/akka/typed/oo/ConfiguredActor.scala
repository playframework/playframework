/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

// #oo-configured-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AbstractBehavior
import javax.inject.Inject
import play.api.Configuration

object ConfiguredActor {
  final case class GetConfig(replyTo: ActorRef[String])
}

final class ConfiguredActor @Inject()(configuration: Configuration)
    extends AbstractBehavior[ConfiguredActor.GetConfig] {
  import ConfiguredActor._
  val config = configuration.get[String]("my.config")
  def onMessage(msg: GetConfig) = {
    msg.replyTo ! config
    this
  }
}
// #oo-configured-actor
