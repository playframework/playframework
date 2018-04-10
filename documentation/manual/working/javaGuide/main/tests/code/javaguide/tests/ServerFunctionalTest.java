/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests;

import java.util.OptionalInt;
import java.util.concurrent.*;

import org.junit.*;

import play.test.*;
import play.libs.ws.*;
import scala.Option;

import static org.junit.Assert.*;

import static play.test.Helpers.NOT_FOUND;

// #test-withserver
public class ServerFunctionalTest extends WithServer {

    @Test
    public void testInServer() throws Exception {
        OptionalInt optHttpPort = testServer.getRunningHttpPort();
        int port = optHttpPort.orElseGet(
                () -> testServer.getRunningHttpsPort().orElseThrow(
                        () -> new IllegalStateException("Both HTTP and HTTPS ports are not provided")
                )
        );
        boolean isHttpConn = optHttpPort.isPresent();
        String url = (isHttpConn ? "http://" : "https://") + "localhost:" + port + "/";
        try (WSClient ws = play.test.WSTestClient.newClient(port)) {
            CompletionStage<WSResponse> stage = ws.url(url).get();
            WSResponse response = stage.toCompletableFuture().get();
            assertEquals(NOT_FOUND, response.getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
// #test-withserver
