/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di;

public class MessageQueue {
  public static boolean stopped = false;

  public static MessageQueue connect() {
    return new MessageQueue();
  }

  public void stop() {
    stopped = true;
  }
}
