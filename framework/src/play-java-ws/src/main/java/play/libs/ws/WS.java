/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;

import com.ning.http.client.AsyncHttpClientConfig;
import play.Application;
import play.libs.ws.ning.NingWSClient;

/**
 * Asynchronous API to to query web services, as an http client.
 */
public class WS {

    /**
     * Returns the default WSClient object managed by the Play application.
     *
     * @return a configured WSClient
     */
    public static WSClient client() {
        Application app = play.Play.application();
        return app.injector().instanceOf(WSClient.class);
    }

    /**
     * Returns a WSRequest object representing the URL.  You can append additional
     * properties on the WSRequest by chaining calls, and execute the request to
     * return an asynchronous {@code Promise<WSResponse>}.
     *
     * @param url the URL to request
     */
    public static WSRequest url(String url) {
        return client().url(url);
    }

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
     */
    public static WSClient newClient(int port) {
        WSClient client = new NingWSClient(new AsyncHttpClientConfig.Builder()
            .setMaxRequestRetry(0).build());

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
            public void close() {
                client.close();
            }
        };
    }

}



