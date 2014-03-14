package java;

// #javaexample
import play.server.ApplicationProvider;
import play.server.SSLContextProvider;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

public class CustomSSLContextProvider implements SSLContextProvider {

    @Override
    public SSLContext createSSLContext(ApplicationProvider applicationProvider) {
        try {
            // change it to your custom implementation
            return SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
// #javaexample
