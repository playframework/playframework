/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;

/**
 * This is the WS Client interface.
 */
public interface WSClient extends java.io.Closeable {

    public Object getUnderlying();

    /**
     * Returns a WSRequest object representing the URL.  You can append additional
     * properties on the WSRequest by chaining calls, and execute the request to
     * return an asynchronous {@code Promise<WSResponse>}.
     *
     * @param url the URL to request
     */
    WSRequest url(String url);

    /**
     * Closes this client, and releases underlying resources.
     *
     * Use this for manually instantiated clients.
     */
    public void close();
}
