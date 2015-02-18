/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;

public interface WSClient extends java.io.Closeable {

    public Object getUnderlying();

    WSRequestHolder url(String url);

    /** Closes this client, and releases underlying resources. */
    public void close();
}
