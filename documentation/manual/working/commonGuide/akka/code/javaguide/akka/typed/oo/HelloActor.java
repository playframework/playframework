/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed.oo;

// #oo-hello-actor
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
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

  public static Behavior<HelloActor.SayHello> create() {
    return Behaviors.setup((ctx) -> new HelloActor(ctx));
  }

  private HelloActor(ActorContext<HelloActor.SayHello> context) {
    super(context);
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
