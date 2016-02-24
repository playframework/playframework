/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.server.api

import javax.net.ssl.SSLEngine

/**
 * To configure the SSLEngine used by Play as a server, extend this class.  In particular, if you want to call
 * sslEngine.setNeedClientAuth(true), this is the place to do it.
 *
 * If you want to specify your own SSL engine, define a class implementing this interface.  If the implementing class
 * takes ApplicationProvider in the constructor, then the applicationProvider is passed into it, if available.
 *
 * The path to this class should be configured with the system property <pre>play.http.sslengineprovider</pre>
 */
trait SSLEngineProvider extends play.server.SSLEngineProvider {

  /**
   * @return the SSL engine to be used for HTTPS connection.
   */
  def createSSLEngine: SSLEngine

}
