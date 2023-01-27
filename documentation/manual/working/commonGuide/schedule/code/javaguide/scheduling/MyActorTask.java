/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

// ###replace: package tasks;
package javaguide.scheduling;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

public class MyActorTask {

  private final ActorRef someActor;
  private final ActorSystem actorSystem;
  private final ExecutionContext executionContext;

  @Inject
  public MyActorTask(
      @Named("some-actor") ActorRef someActor,
      ActorSystem actorSystem,
      ExecutionContext executionContext) {
    this.someActor = someActor;
    this.actorSystem = actorSystem;
    this.executionContext = executionContext;

    this.initialize();
  }

  private void initialize() {
    actorSystem
        .scheduler()
        .scheduleAtFixedRate(
            Duration.create(0, TimeUnit.SECONDS), // initialDelay
            Duration.create(30, TimeUnit.SECONDS), // interval
            someActor,
            "tick", // message,
            executionContext,
            ActorRef.noSender());
  }
}
