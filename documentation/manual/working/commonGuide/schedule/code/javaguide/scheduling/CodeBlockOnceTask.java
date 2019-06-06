/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
// ###skip: 9
package javaguide.scheduling;

import akka.actor.ActorSystem;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class CodeBlockOnceTask {

  private final ActorSystem actorSystem;
  private final ExecutionContext executionContext;

  @Inject
  public CodeBlockOnceTask(ActorSystem actorSystem, ExecutionContext executionContext) {
    this.actorSystem = actorSystem;
    this.executionContext = executionContext;

    this.initialize();
  }

  private void initialize() {
    this.actorSystem
        .scheduler()
        .scheduleOnce(
            Duration.create(10, TimeUnit.SECONDS), // delay
            () -> System.out.println("Running just once."),
            this.executionContext);
  }
}
