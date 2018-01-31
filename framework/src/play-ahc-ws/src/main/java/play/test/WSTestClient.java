/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import play.libs.ws.ahc.StandaloneAhcWSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClientConfig;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClientConfig;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.ahc.AhcWSClient;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class WSTestClient {

    // This is used to create fresh names when creating `ActorMaterializer` instances in `WsTestClient.withClient`.
    // The motivation is that it can be useful for debugging.
    private static AtomicInteger instanceNumber = new AtomicInteger(1);

    /**
     * Create a new WSClient for use in testing.
     * <p>
     * This client holds on to resources such as connections and threads, and so must be closed after use.
     * <p>
     * If the URL passed into the url method of this client is a host relative absolute path (that is, if it starts
     * with /), then this client will make the request on localhost using the supplied port.  This is particularly
     * useful in test situations.
     *
     * @param port The port to use on localhost when relative URLs are requested.
     * @return A running WS client.
     */
    public static WSClient newClient(final int port) {
        AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setMaxRequestRetry(0).setShutdownQuietPeriod(0).setShutdownTimeout(0).build();

        String name = "ws-test-client-" + instanceNumber.getAndIncrement();
        final ActorSystem system = ActorSystem.create(name);
        ActorMaterializerSettings settings = ActorMaterializerSettings.create(system);
        ActorMaterializer materializer = ActorMaterializer.create(settings, system, name);

        final AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(config);
        final WSClient client = new AhcWSClient(asyncHttpClient, materializer);

        return new WSClient() {
            public Object getUnderlying() {
                return client.getUnderlying();
            }

            public WSRequest url(String url) {
                if (url.startsWith("/") && port != -1) {
                    return client.url("http://localhost:" + port + url);
                } else {
                    return client.url(url);
                }
            }

            public void close() throws IOException {
                try {
                    try {
                        client.close();
                    } finally {
                        final Future<Terminated> terminate = system.terminate();
                        Await.result(terminate, Duration.Inf());
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

            @Override
            public play.api.libs.ws.WSClient asScala() {
                return new play.api.libs.ws.ahc.AhcWSClient(
                    new play.api.libs.ws.ahc.StandaloneAhcWSClient(
                        asyncHttpClient,
                        materializer
                    )
                );
            }
        };
    }
}
