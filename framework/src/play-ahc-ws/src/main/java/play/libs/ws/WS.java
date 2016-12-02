/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;

import java.io.IOException;

import org.asynchttpclient.AsyncHttpClientConfig;

import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import play.libs.ws.ahc.AhcWSClient;

/**
 * Asynchronous API to query web services, as an http client.
 *
 * @deprecated as of 2.6, this class is deprecated in favor of WSAPI.
 */
@Deprecated
public class WS {

    /**
     * Create a new WSClient.
     *
     * This client holds on to resources such as connections and threads, and so must be closed after use.
     *
     * If the URL passed into the url method of this client is a host relative absolute path (that is, if it starts
     * with /), then this client will make the request on localhost using the supplied port.  This is particularly
     * useful in test situations.
     *
     * @param port The port to use on localhost when relative URLs are requested.
     * @return A running WS client.
     * @deprecated as of 2.6, not to be used.
     *    This method is not appropriate outside of testing context, because it makes many
     *    assumptions about the url, starts up a new client and actorsystem on every
     *    call, and hardcodes the config to something other than the app config.
     */
    @Deprecated
    public static WSClient newClient(final int port) {
        AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setMaxRequestRetry(0).setShutdownQuietPeriod(0).setShutdownTimeout(0).build();

        String name = "ws-java-newClient";
        final ActorSystem system = ActorSystem.create(name);
        ActorMaterializerSettings settings = ActorMaterializerSettings.create(system);
        ActorMaterializer materializer = ActorMaterializer.create(settings, system, name);

        final WSClient client = new AhcWSClient(config, materializer);

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
                    client.close();
                }
                finally {
                    system.terminate();
                }
            }
        };
    }

}



