/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed.fp;

// #fp-hello-actor
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

public final class HelloActor {

  public static final class SayHello {
    public final String name;
    public final ActorRef<String> replyTo;

    public SayHello(String name, ActorRef<String> replyTo) {
      this.name = name;
      this.replyTo = replyTo;
    }
  }

  public static Behavior<HelloActor.SayHello> create() {
    return Behaviors.receiveMessage(
        (SayHello message) -> {
          message.replyTo.tell("Hello, " + message.name);
          return Behaviors.same();
        });
  }
}
// #fp-hello-actor
