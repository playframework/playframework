/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package play.libs.ws.ning;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

/**
 * A WS client backed by a Ning AsyncHttpClient.
 *
 * If you need to debug Ning, set logger.com.ning.http.client=DEBUG in your application.conf file.
 */
public class NingWSClient implements WSClient {

    private final AsyncHttpClient asyncHttpClient;

    public NingWSClient(AsyncHttpClientConfig config) {
        this.asyncHttpClient = new AsyncHttpClient(config);
    }

    @Override
    public Object getUnderlying() {
        return asyncHttpClient;
    }

    @Override
    public WSRequest url(String url) {
        return new NingWSRequest(this, url);
    }

    @Override
    public void close() {
        asyncHttpClient.close();
    }
}
