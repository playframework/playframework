/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it;

import org.junit.Test;
import static org.junit.Assert.*;

import play.routing.Router;
import play.server.Server;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class JavaServerIntegrationTest {
    @Test
    public void testHttpEmbeddedServerUsesCorrectProtocolAndPort() throws Exception {
        int port = _availablePort();
        _running(new Server.Builder().http(port).build(_emptyRouter()), server -> {
            assertTrue(_isPortOccupied(port));
            assertFalse(_isServingSSL(port));
            assertEquals(server.httpPort(), port);
            try {
                server.httpsPort();
                fail("Exception should be thrown on accessing https port of server that is not serving that protocol");
            } catch (IllegalStateException e) {}
        });
        assertFalse(_isPortOccupied(port));
    }

    @Test
    public void testHttpsEmbeddedServerUsesCorrectProtocolAndPort() throws Exception {
        int port = _availablePort();
        _running(new Server.Builder().https(port).build(_emptyRouter()), server -> {
            assertEquals(server.httpsPort(), port);
            assertTrue(_isServingSSL(port));

            try {
                server.httpPort();
                fail("Exception should be thrown on accessing http port of server that is not serving that protocol");
            } catch (IllegalStateException e) {
            }
        });
        assertFalse(_isPortOccupied(port));
    }

    @Test
    public void testEmbeddedServerCanServeBothProtocolsSimultaneously() throws Exception {
        List<Integer> availablePorts = _availablePorts(2);
        int httpPort = availablePorts.get(0);
        int httpsPort = availablePorts.get(1);

        _running(new Server.Builder().http(httpPort).https(httpsPort).build(_emptyRouter()), server -> {
            // HTTP port should be serving http in the clear
            assertTrue(_isPortOccupied(httpPort));
            assertFalse(_isServingSSL(httpPort));
            assertEquals(server.httpPort(), httpPort);

            // HTTPS port should be serving over SSL
            assertTrue(_isPortOccupied(httpsPort));
            assertTrue(_isServingSSL(httpsPort));
            assertEquals(server.httpsPort(), httpsPort);
        });

        assertFalse(_isPortOccupied(httpPort));
        assertFalse(_isPortOccupied(httpsPort));
    }

    @Test
    public void testEmbeddedServerWillChooseAnHTTPPortIfNotProvided() throws Exception {
        _running(new Server.Builder().build(_emptyRouter()), server -> {
            assertTrue(_isPortOccupied(server.httpPort()));
        });
    }

    //
    // Private helpers
    //
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
        return _availablePorts(1).get(0);
    }

    private List<Integer> _availablePorts(int n) throws IOException {
        List<ServerSocket> sockets = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ServerSocket socket = new ServerSocket(0);
            sockets.add(socket);
        }

        List<Integer> portNumbers = new ArrayList<>();
        for (ServerSocket socket : sockets) {
            portNumbers.add(socket.getLocalPort());
            socket.close();
        }

        return portNumbers;
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
        return Router.empty();
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
