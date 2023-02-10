/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.fp

// #fp-hello-actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior

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
