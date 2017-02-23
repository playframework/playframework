/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ning;

import akka.stream.Materializer;

import java.io.IOException;

import org.asynchttpclient.AsyncHttpClientConfig;

import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.ahc.AhcWSClient;

/**
 * A WS client backed by an AsyncHttpClient.
 *
 * If you need to debug AHC, set org.asynchttpclient=DEBUG in your logging framework.
 *
 * @deprecated Use AhcWSClient instead
 */
@Deprecated
public class NingWSClient implements WSClient {

    private final AhcWSClient ahc;

    public NingWSClient(AsyncHttpClientConfig config, Materializer materializer) {
        this.ahc = new AhcWSClient(config, materializer);
    }

    @Override
    public Object getUnderlying() {
        return ahc.getUnderlying();
    }

    @Override
    public WSRequest url(String url) {
        return ahc.url(url);
    }

    @Override
    public void close() throws IOException {
        ahc.close();
    }
}
