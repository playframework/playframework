/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server;

/**
 * A server that can be reloaded or stopped.
 */
public interface ReloadableServer {

  /**
   * Stop the server.
   */
  void stop();

  /**
   * Reload the server if necessary.
   */
  void reload();

  /**
   * Get the address of the server.
   *
   * @return The address of the server.
   */
  java.net.InetSocketAddress mainAddress(); 

}
