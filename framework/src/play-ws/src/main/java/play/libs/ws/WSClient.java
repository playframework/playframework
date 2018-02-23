/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws;

import java.io.IOException;

/**
 * This is the WS Client interface for Java.
 *
 * Most of the time you will access this through dependency injection, i.e.
 *
 * <pre>
 * <code>import javax.inject.Inject;
 * import play.libs.ws.*;
 * import java.util.concurrent.CompletionStage;
 *
 * public class MyService {
 *   {@literal @}Inject WSClient ws;
 *
 *    // ...
 * }
 * </code>
 * </pre>
 *
 * Please see https://www.playframework.com/documentation/latest/JavaWS for more details.
 */
public interface WSClient extends java.io.Closeable {

    /**
     * The underlying implementation of the client, if any.  You must cast the returned value to the type you want.
     *
     * @return the backing object.
     */
    Object getUnderlying();

    /**
     * @return the Scala version for this WSClient.
     */
    play.api.libs.ws.WSClient asScala();

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
     * <p>
     * Use this for manually instantiated clients.
     */
    void close() throws IOException;
}
