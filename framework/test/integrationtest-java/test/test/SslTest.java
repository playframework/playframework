package test;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.api.test.Helpers;
import play.test.TestServer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

public class SslTest {
    private static final int SSL_PORT = 8443;

    @After
    public void tearDown() {
        System.getProperties().remove("https.trustStore");
    }

    @Test
    public void testClientCerts() {
        System.setProperty("https.trustStore", "noCA");
        running(new TestServer(-1, fakeApplication(), SSL_PORT), new Runnable() {
            @Override
            public void run() {
                try {
                    // Setup an HTTP connection with our client certificate
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    InputStream is = new FileInputStream("conf/bobclient.p12");
                    try {
                        keyStore.load(is, "password".toCharArray());
                    } finally {
                        IOUtils.closeQuietly(is);
                    }
                    kmf.init(keyStore, "password".toCharArray());
                    SSLContext ctx = SSLContext.getInstance("TLS");
                    ctx.init(kmf.getKeyManagers(), new X509TrustManager[]{new MockTrustManager()}, null);
                    HttpsURLConnection conn = (HttpsURLConnection) new URL("https://localhost:" + SSL_PORT + "/clientCert").openConnection();
                    conn.setSSLSocketFactory(ctx.getSocketFactory());

                    // Now use it
                    assertEquals(200, conn.getResponseCode());
                    assertEquals("Bob Client", IOUtils.toString(conn.getInputStream()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    static class MockTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] certs, String s) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String s) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
