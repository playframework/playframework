/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
