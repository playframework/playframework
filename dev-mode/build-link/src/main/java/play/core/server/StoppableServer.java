/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server;

/** A server that can be reloaded or stopped. */
public interface StoppableServer {

  /** Stop the server. */
  void stop();
}
