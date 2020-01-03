/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server;

/** A server that can be reloaded or stopped. */
public interface ReloadableServer {

  /** Stop the server. */
  void stop();

  /** Reload the server if necessary. */
  void reload();
}
