/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di;

// #cleanup
import java.util.concurrent.CompletableFuture;
import javax.inject.*;
import play.inject.ApplicationLifecycle;

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
