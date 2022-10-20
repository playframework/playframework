/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.async;

// #actor
import akka.actor.*;

public class MyWebSocketActor extends AbstractActor {

  public static Props props(ActorRef out) {
    return Props.create(MyWebSocketActor.class, out);
  }

  private final ActorRef out;

  public MyWebSocketActor(ActorRef out) {
    this.out = out;
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(String.class, message -> out.tell("I received your message: " + message, self()))
        .build();
  }
}
// #actor
