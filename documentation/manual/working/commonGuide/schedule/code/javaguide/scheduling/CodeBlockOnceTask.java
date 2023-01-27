/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

// ###skip: 9
package javaguide.scheduling;

import akka.actor.ActorSystem;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

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
