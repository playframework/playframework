/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.advanced.embedding;

import java.io.IOException;

import org.asynchttpclient.AsyncHttpClientConfig;
import org.junit.Test;

import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.libs.ws.ahc.AhcWSClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

//#imports
import play.api.Play;
import play.Mode;
import play.routing.RoutingDsl;
import play.server.Server;
import static play.mvc.Controller.*;
import akka.stream.Materializer;
//#imports

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class JavaEmbeddingPlay {

    @Test
    public void simple() throws Exception {
        //#simple
        Server server = Server.forRouter(new RoutingDsl()
            .GET("/hello/:to").routeTo(to ->
                ok("Hello " + to)
            )
            .build()
        );
        //#simple

        try {
            withClient(ws -> {
                //#http-port
                CompletionStage<WSResponse> response = ws.url(
                    "http://localhost:" + server.httpPort() + "/hello/world"
                ).get();
                //#http-port
                try {
                    assertThat(response.toCompletableFuture().get(10, TimeUnit.SECONDS).getBody(), equalTo("Hello world"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            //#stop
            server.stop();
            //#stop
        }
    }

    @Test
    public void config() throws Exception {
        //#config
        Server server = Server.forRouter(new RoutingDsl()
            .GET("/hello/:to").routeTo(to ->
                ok("Hello " + to)
            )
            .build(),
            Mode.TEST, 19000
        );
        //#config

        try {
            withClient(ws -> {
                    try {
                        assertThat(ws.url("http://localhost:19000/hello/world").get().toCompletableFuture().get(10,
                                TimeUnit.SECONDS).getBody(), equalTo("Hello world"));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            );
        } finally {
            server.stop();
        }
    }

    private void withClient(Consumer<WSClient> callback) throws IOException {
        try (WSClient client = play.libs.ws.WS.newClient(19000)) {
            callback.accept(client);
        }
    }

}
