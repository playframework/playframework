/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server;

/**
 * A server that can be stopped.
 */
public interface ServerWithStop {

  /**
   * Stop the server.
   */
  public void stop();

  /**
   * Get the address of the server.
   *
   * @return The address of the server.
   */
  public java.net.InetSocketAddress mainAddress(); 

}
