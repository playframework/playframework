package play.server;

import javax.net.ssl.SSLContext;

/**
 * To listen on HTTPS port, play needs a SSL context.
 * If you want specify your own SSL context, define a class implementing this interface.
 * The path to this class should be configured with the system property <pre>play.http.sslcontextprovider</pre>
 */
public interface SSLContextProvider {

    /**
     * @return the SSL context to be used for HTTPS connection.
     */
    SSLContext createSSLContext(ApplicationProvider applicationProvider);
}
