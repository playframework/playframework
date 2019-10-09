/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed.oo.multi;

import akka.actor.typed.ActorRef;
import javaguide.akka.typed.oo.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class Main {
  public final ActorRef<HelloActor.SayHello> helloActor1;
  public final ActorRef<HelloActor.SayHello> helloActor2;
  public final ActorRef<ConfiguredActor.GetConfig> configuredActor1;
  public final ActorRef<ConfiguredActor.GetConfig> configuredActor2;

  @Inject
  public Main(
      @Named("hello-actor1") ActorRef<HelloActor.SayHello> helloActor1,
      @Named("hello-actor2") ActorRef<HelloActor.SayHello> helloActor2,
      @Named("configured-actor1") ActorRef<ConfiguredActor.GetConfig> configuredActor1,
      @Named("configured-actor2") ActorRef<ConfiguredActor.GetConfig> configuredActor2) {
    this.helloActor1 = helloActor1;
    this.helloActor2 = helloActor2;
    this.configuredActor1 = configuredActor1;
    this.configuredActor2 = configuredActor2;
  }
}
