/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// ###skip: 9
package javaguide.scheduling;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.apache.pekko.actor.ActorSystem;
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
