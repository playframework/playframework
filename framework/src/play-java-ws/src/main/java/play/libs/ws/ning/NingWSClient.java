/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package play.libs.ws.ning;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

public class NingWSClient implements WSClient {

    private AsyncHttpClient asyncHttpClient;

    public NingWSClient(AsyncHttpClientConfig config) {
        this.asyncHttpClient = new AsyncHttpClient(config);
    }

    public Object getUnderlying() {
        return this.asyncHttpClient;
    }

    @Override
    public WSRequest url(String url) {
        return new NingWSRequest(this, url);
    }

    public void close() {
        this.asyncHttpClient.close();
    }
}
