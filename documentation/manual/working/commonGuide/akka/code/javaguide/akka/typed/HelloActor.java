/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed;

// #oo-hello-actor
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Receive;

public final class HelloActor extends AbstractBehavior<HelloActor.SayHello> {

  public static final class SayHello {
    public final String name;
    public final ActorRef<String> replyTo;

    public SayHello(String name, ActorRef<String> replyTo) {
      this.name = name;
      this.replyTo = replyTo;
    }
  }

  @Override
  public Receive<SayHello> createReceive() {
    return newReceiveBuilder().onMessage(SayHello.class, this::onHello).build();
  }

  private Behavior<SayHello> onHello(SayHello message) {
    message.replyTo.tell("Hello, " + message.name);
    return this;
  }
}
// #oo-hello-actor
