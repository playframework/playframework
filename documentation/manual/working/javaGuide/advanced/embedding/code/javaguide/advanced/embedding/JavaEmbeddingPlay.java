/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.embedding;

import com.ning.http.client.AsyncHttpClientConfig;
import org.junit.Test;

import play.libs.F;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.libs.ws.ning.NingWSClient;
import java.util.function.Consumer;

//#imports
import play.Mode;
import play.routing.RoutingDsl;
import play.server.Server;
import static play.mvc.Controller.*;
//#imports

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class JavaEmbeddingPlay {

    @Test
    public void simple() {
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
                F.Promise<WSResponse> response = ws.url(
                    "http://localhost:" + server.httpPort() + "/hello/world"
                ).get();
                //#http-port
                assertThat(response.get(10000).getBody(), equalTo("Hello world"));
            });
        } finally {
            //#stop
            server.stop();
            //#stop
        }
    }

    @Test
    public void config() {
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
            withClient(ws ->
                assertThat(ws.url("http://localhost:19000/hello/world").get().get(10000).getBody(), equalTo("Hello world"))
            );
        } finally {
            server.stop();
        }
    }

    private void withClient(Consumer<WSClient> callback) {
        try (WSClient client = new NingWSClient(new AsyncHttpClientConfig.Builder().build())) {
            callback.accept(client);
        }
    }

}
