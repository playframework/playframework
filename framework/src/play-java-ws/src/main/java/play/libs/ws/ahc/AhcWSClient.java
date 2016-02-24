/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import akka.stream.Materializer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import java.io.IOException;

/**
 * A WS client backed by an AsyncHttpClient.
 *
 * If you need to debug AHC, set org.asynchttpclient=DEBUG in your logging framework.
 */
public class AhcWSClient implements WSClient {

    private final AsyncHttpClient asyncHttpClient;
    private final Materializer materializer;

    public AhcWSClient(AsyncHttpClientConfig config, Materializer materializer) {
        this.asyncHttpClient = new DefaultAsyncHttpClient(config);
        this.materializer = materializer;
    }

    @Override
    public Object getUnderlying() {
        return asyncHttpClient;
    }

    @Override
    public WSRequest url(String url) {
        return new AhcWSRequest(this, url, materializer);
    }

    @Override
    public void close() throws IOException {
        asyncHttpClient.close();
    }
}
