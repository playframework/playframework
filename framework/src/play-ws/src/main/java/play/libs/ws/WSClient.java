/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws;

import java.io.IOException;

/**
 * This is the WS Client interface.
 */
public interface WSClient extends java.io.Closeable {

    /**
     * The underlying implementation of the client, if any.  You must cast the returned value to the type you want.
     * @return the backing class.
     */
    Object getUnderlying();

    /**
     * Returns a WSRequest object representing the URL.  You can append additional
     * properties on the WSRequest by chaining calls, and execute the request to
     * return an asynchronous {@code Promise<WSResponse>}.
     *
     * @param url the URL to request
     * @return the request
     */
    WSRequest url(String url);

    /**
     * Closes this client, and releases underlying resources.
     *
     * Use this for manually instantiated clients.
     */
    void close() throws IOException;
}
