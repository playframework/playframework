/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di;

// #cleanup
import javax.inject.*;
import play.inject.ApplicationLifecycle;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

@Singleton
public class MessageQueueConnection {
  private final MessageQueue connection;

  @Inject
  public MessageQueueConnection(ApplicationLifecycle lifecycle) {
    connection = MessageQueue.connect();

    lifecycle.addStopHook(
        () -> {
          connection.stop();
          return CompletableFuture.completedFuture(null);
        });
  }

  // ...
}
// #cleanup
