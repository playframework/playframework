/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it;

import org.junit.Test;
import static org.junit.Assert.*;

import play.Mode;
import play.api.routing.Router;
import play.test.Helpers;
import play.server.Server;
import play.server.Server.PortConfig;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class JavaServerIntegrationTest {
    @Test
    public void testHttpEmbeddedServerUsesCorrectProtocolAndPort() throws Exception {
        int port = _availablePort();
        _running(Server.forRouter(_emptyRouter(), port), server -> {
            assertTrue(_isPortOccupied(port));
            assertFalse(_isServingSSL(port));
            assertEquals(server.httpPort(), port);
            try {
                server.httpsPort();
                fail("Exception should be thrown on accessing https port of server started in http mode");
            } catch (IllegalStateException e) {}
        });
        assertFalse(_isPortOccupied(port));
    }

    @Test
    public void testHttpsEmbeddedServerUsesCorrectProtocolAndPort() throws Exception {
        int port = _availablePort();
        _running(Server.forRouter(_emptyRouter(), Mode.DEV, PortConfig.https(port)), server -> {
            assertEquals(server.httpsPort(), port);
            assertTrue(_isServingSSL(port));

            try {
                server.httpPort();
                fail("Exception should be thrown ona ccessing http port of server started in https mode");
            } catch (IllegalStateException e) {}
        });
        assertFalse(_isPortOccupied(port));
    }

    private void _running(Server server, ServerRunnable runnable) throws Exception {
        try {
            runnable.run(server);
        } finally {
            server.stop();
        }
    }

    private interface ServerRunnable {
        void run(Server server) throws Exception;
    }

    private int _availablePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();

        return port;
    }

    private boolean _isServingSSL(int port) throws IOException {
        // Inspired by @4ndrej's SSLPoke https://gist.github.com/4ndrej/4547029
        try {
            SSLSocket sslsocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket("127.0.0.1", port);
            InputStream in = sslsocket.getInputStream();
            OutputStream out = sslsocket.getOutputStream();

            // Write a test byte to get a reaction :)
            out.write(1);

            while (in.available() > 0) {
                in.read();
            }

            in.close();
            out.close();

            return true;
        } catch (SSLHandshakeException e) {
            // If it started handshaking then the port was definitely serving ssl
            return true;
        } catch (SSLException e) {
            // Any other ssl exception probably means it wasn't serving SSL
            return false;
        }
    }

    private Router _emptyRouter() {
        return Helpers.fakeApplication().getWrappedApplication().routes();
    }

    private boolean _isPortOccupied(int port) {
        try {
            Socket s = new Socket("127.0.0.1", port);
            s.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
