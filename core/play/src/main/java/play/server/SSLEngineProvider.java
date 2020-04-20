/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.server;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * To configure the SSLEngine used by Play as a server, extend this class. In particular, if you
 * want to call sslEngine.setNeedClientAuth(true), this is the place to do it.
 *
 * <p>If you want to specify your own SSL engine, define a class implementing this interface. If the
 * implementing class takes ApplicationProvider in the constructor, then the applicationProvider is
 * passed into it, if available.
 *
 * <p>The path to this class should be configured with the system property
 *
 * <pre>play.server.https.engineProvider</pre>
 */
public interface SSLEngineProvider {

  /** @return the SSL engine to be used for HTTPS connection. */
  SSLEngine createSSLEngine();

  /**
   * The {@link SSLContext} used to create the SSLEngine.
   *
   * @see #createSSLEngine()
   */
  SSLContext sslContext();
}
