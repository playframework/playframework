/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package play.libs.ws.ning;

import akka.stream.Materializer;

import java.io.IOException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;

import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

/**
 * A WS client backed by a Ning AsyncHttpClient.
 *
 * If you need to debug Ning, set logger.com.ning.http.client=DEBUG in your application.conf file.
 */
public class NingWSClient implements WSClient {

    private final AsyncHttpClient asyncHttpClient;
    private final Materializer materializer;

    public NingWSClient(AsyncHttpClientConfig config, Materializer materializer) {
        this.asyncHttpClient = new DefaultAsyncHttpClient(config);
        this.materializer = materializer;
    }

    @Override
    public Object getUnderlying() {
        return asyncHttpClient;
    }

    @Override
    public WSRequest url(String url) {
        return new NingWSRequest(this, url, materializer);
    }

    @Override
    public void close() throws IOException {
        asyncHttpClient.close();
    }
}
