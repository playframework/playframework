/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.pekko.typed.fp

// #fp-hello-actor
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior

object HelloActor {
  final case class SayHello(
      name: String,
      replyTo: ActorRef[String],
  )

  def create(): Behavior[SayHello] = {
    Behaviors.receiveMessage[SayHello] {
      case SayHello(name, replyTo) =>
        replyTo ! s"Hello, $name"
        Behaviors.same
    }
  }
}
// #fp-hello-actor
