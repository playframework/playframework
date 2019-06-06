/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
// #actor
// ###replace: package actors;
package javaguide.akka;

import akka.actor.*;
import akka.japi.*;
// ###replace: import actors.HelloActorProtocol.*;
import javaguide.akka.HelloActorProtocol.*;

public class HelloActor extends AbstractActor {

  public static Props getProps() {
    return Props.create(HelloActor.class);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            SayHello.class,
            hello -> {
              String reply = "Hello, " + hello.name;
              sender().tell(reply, self());
            })
        .build();
  }
}
// #actor
