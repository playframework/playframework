/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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
 * If you need to debug Ahc, set logger.org.asynchttpclient=DEBUG in your application.conf file.
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
