/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

// #oo-hello-actor
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior

object HelloActor {
  final case class SayHello(
      name: String,
      replyTo: ActorRef[String],
  )

  def create(): Behavior[HelloActor.SayHello] = {
    Behaviors.setup(context => new HelloActor(context))
  }
}

final class HelloActor private (
    context: ActorContext[HelloActor.SayHello],
) extends AbstractBehavior(context) {
  import HelloActor._

  def onMessage(msg: SayHello): HelloActor = {
    msg.replyTo ! s"Hello, ${msg.name}"
    this
  }
}
// #oo-hello-actor
