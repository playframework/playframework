/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;

import play.Application;

/**
 * Asynchronous API to to query web services, as an http client.
 *
 * The value returned is a {@code Promise<Response>}, and you should use Play's asynchronous mechanisms to use this response.
 */
public class WS {

    public static WSClient client() {
        Application app = play.Play.application();
        WSPlugin wsPlugin = app.plugin(WSPlugin.class);
        if (wsPlugin.enabled() && wsPlugin.loaded()) {
            return wsPlugin.api().client();
        } else {
            throw new IllegalStateException("WSPlugin is not loaded / enabled!");
        }
    }

    /**
     * Prepare a new request. You can then construct it by chaining calls.
     *
     * @param url the URL to request
     */
    public static WSRequestHolder url(String url) {
        return client().url(url);
    }

}



