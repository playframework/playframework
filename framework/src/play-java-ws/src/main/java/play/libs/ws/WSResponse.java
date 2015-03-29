/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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
     * Gets all the headers from the response.
     */
    Map<String, List<String>> getAllHeaders();

    /**
     * Gets a single header from the response.
     */
    String getHeader(String key);

    /**
     * Gets the underlying implementation response object, if any.
     */
    Object getUnderlying();

    /**
     * Returns the HTTP status code from the response.
     */
    int getStatus();

    /**
     * Returns the text associated with the status code.
     */
    String getStatusText();

    /**
     * Gets all the cookies from the response.
     */
    List<WSCookie> getCookies();

    /**
     * Gets a single cookie from the response, if any.
     */
    WSCookie getCookie(String name);

    /**
     * Gets the body as a string.
     */
    String getBody();

    /**
     * Gets the body as XML.
     */
    Document asXml();

    /**
     * Gets the body as JSON node.
     */
    JsonNode asJson();

    /**
     * Gets the body as a stream.
     */
    InputStream getBodyAsStream();

    /**
     * Gets the body as an array of bytes.
     */
    byte[] asByteArray();

    /**
     * Gets the URI of the response.
     */
    URI getUri();
}
