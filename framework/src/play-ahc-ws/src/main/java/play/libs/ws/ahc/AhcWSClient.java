/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import akka.stream.Materializer;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;

import javax.inject.Inject;
import java.io.IOException;

/**
 * A WS client backed by AsyncHttpClient.
 */
public class AhcWSClient implements WSClient {

    private final StandaloneAhcWSClient client;

    @Inject
    public AhcWSClient(StandaloneAhcWSClient client) {
        this.client = client;
    }

    /**
     * Secondary constructor for manually created AhcWSClient
     *
     * @param asyncHttpClient an AHC instance
     * @param materializer an Akka materializer
     */
    public AhcWSClient(AsyncHttpClient asyncHttpClient, Materializer materializer) {
        this.client = new StandaloneAhcWSClient(asyncHttpClient, materializer);
    }

    @Override
    public Object getUnderlying() {
        return client.getUnderlying();
    }

    @Override
    public WSRequest url(String url) {
        final StandaloneAhcWSRequest plainWSRequest = client.url(url);
        return new AhcWSRequest(this, plainWSRequest);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
