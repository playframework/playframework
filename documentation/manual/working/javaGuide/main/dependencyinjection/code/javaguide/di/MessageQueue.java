/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
