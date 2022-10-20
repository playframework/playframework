/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// ###replace: package tasks;
package javaguide.scheduling;

import akka.actor.ActorSystem;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

// #schedule-block-with-interval
public class CodeBlockTask {

  private final ActorSystem actorSystem;
  private final ExecutionContext executionContext;

  @Inject
  public CodeBlockTask(ActorSystem actorSystem, ExecutionContext executionContext) {
    this.actorSystem = actorSystem;
    this.executionContext = executionContext;

    this.initialize();
  }

  private void initialize() {
    this.actorSystem
        .scheduler()
        .scheduleAtFixedRate(
            Duration.create(10, TimeUnit.SECONDS), // initialDelay
            Duration.create(1, TimeUnit.MINUTES), // interval
            () -> actorSystem.log().info("Running block of code"),
            this.executionContext);
  }
}
// #schedule-block-with-interval
