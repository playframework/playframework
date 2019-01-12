/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestServerTest {
    @Test
    public void shouldReturnHttpPort() {
        int testServerPort = play.api.test.Helpers.testServerPort();
        final TestServer testServer = Helpers.testServer(testServerPort);
        testServer.start();
        assertTrue("No value for http port", testServer.getRunningHttpPort().isPresent());
        assertFalse("ssl port value is present, but was not set", testServer.getRunningHttpsPort().isPresent());
        assertEquals(testServerPort, testServer.getRunningHttpPort().getAsInt());
        testServer.stop();
    }

    @Test
    public void shouldReturnHttpAndSslPorts() {
        int port = play.api.test.Helpers.testServerPort();
        int sslPort = port + 1;
        final TestServer testServer = Helpers.testServer(port, sslPort);
        testServer.start();
        assertTrue("No value for ssl port", testServer.getRunningHttpsPort().isPresent());
        assertEquals(sslPort, testServer.getRunningHttpsPort().getAsInt());
        assertTrue("No value for http port", testServer.getRunningHttpPort().isPresent());
        assertEquals(port, testServer.getRunningHttpPort().getAsInt());
        testServer.stop();
    }
}
