/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.pekko.typed.fp

// #fp-configured-actor
import com.google.inject.Provides
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import play.api.libs.concurrent.ActorModule
import play.api.Configuration

object ConfiguredActor extends ActorModule {
  type Message = GetConfig

  final case class GetConfig(replyTo: ActorRef[String])

  @Provides
  def create(configuration: Configuration): Behavior[GetConfig] = {
    Behaviors.setup { _ =>
      val config = configuration.get[String]("my.config")
      Behaviors.receiveMessage[GetConfig] {
        case GetConfig(replyTo) =>
          replyTo ! config
          Behaviors.same
      }
    }
  }
}
// #fp-configured-actor
