/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

// #oo-hello-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

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
