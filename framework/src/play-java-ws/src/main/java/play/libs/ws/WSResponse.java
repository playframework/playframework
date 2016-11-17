/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * This is the WS response from the server.
 */
public interface WSResponse {

    /**
     * @return all the headers from the response.
     */
    Map<String, List<String>> getAllHeaders();

    /**
     * @param key    the header's name
     * @return a single header value from the response.
     */
    String getHeader(String key);

    /**
     * @return the underlying implementation response object, if any.
     */
    Object getUnderlying();

    /**
     * @return the HTTP status code from the response.
     */
    int getStatus();

    /**
     * @return the text associated with the status code.
     */
    String getStatusText();

    /**
     * @return all the cookies from the response.
     */
    List<WSCookie> getCookies();

    /**
     * @param name    the cookie name
     * @return a single cookie from the response, if any.
     */
    WSCookie getCookie(String name);

    /**
     * @return the body as a string.
     */
    String getBody();

    /**
     * @return the body as XML.
     */
    Document asXml();

    /**
     * @return the body as JSON node.
     */
    JsonNode asJson();

    /**
     * @return the body as a stream.
     */
    InputStream getBodyAsStream();

    /**
     * @return the body as an array of bytes.
     */
    byte[] asByteArray();

    /**
     * @return the URI of the response.
     */
    URI getUri();
}
