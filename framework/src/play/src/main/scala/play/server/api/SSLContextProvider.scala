package play.server.api

import javax.net.ssl.SSLContext
import play.core.ApplicationProvider

/**
 * To listen on HTTPS port, play needs a SSL context.
 * If you want specify your own SSL context, define a class implementing this interface.
 * The path to this class should be configured with the system property <pre>play.http.sslcontextprovider</pre>
 */
trait SSLContextProvider {

  /**
   * @return the SSL context to be used for HTTPS connection.
   */
  def createSSLContext(appProvider: ApplicationProvider): SSLContext

}
