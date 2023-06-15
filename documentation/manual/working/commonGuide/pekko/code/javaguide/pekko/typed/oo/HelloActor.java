/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko.typed.oo;

// #oo-hello-actor
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

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
