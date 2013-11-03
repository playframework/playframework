/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package play.libs.ws.ning;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequestHolder;

public class NingWSClient implements WSClient {

    private AsyncHttpClient asyncHttpClient;

    public NingWSClient(AsyncHttpClientConfig config) {
        this.asyncHttpClient = new AsyncHttpClient(config);
    }

    public Object getUnderlying() {
        return this.asyncHttpClient;
    }

    @Override
    public WSRequestHolder url(String url) {
        return new NingWSRequestHolder(this, url);
    }

    protected void close() {
        this.asyncHttpClient.close();
    }
}
