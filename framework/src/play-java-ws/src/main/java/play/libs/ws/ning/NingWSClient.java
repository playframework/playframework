/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
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
 * If you need to debug Ahc, set logger.org.asynchttpclient=DEBUG in your application.conf file.
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
