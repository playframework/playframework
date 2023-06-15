/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko.typed.fp;

// #fp-hello-actor
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

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
