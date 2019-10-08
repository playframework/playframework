/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

// #oo-hello-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext

object HelloActor {
  final case class SayHello(name: String, replyTo: ActorRef[String])
}

final class HelloActor(context: ActorContext[HelloActor.SayHello]) extends AbstractBehavior(context) {
  import HelloActor._
  def onMessage(msg: SayHello) = {
    msg.replyTo ! s"Hello, ${msg.name}"
    this
  }
}
// #oo-hello-actor
