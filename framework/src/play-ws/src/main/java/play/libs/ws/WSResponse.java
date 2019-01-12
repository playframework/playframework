/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws;

import akka.util.ByteString;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This is the WS response from the server.
 */
public interface WSResponse extends StandaloneWSResponse {

    @Override
    Map<String, List<String>> getHeaders();

    @Override
    List<String> getHeaderValues(String name);

    @Override
    Optional<String> getSingleHeader(String name);

    /**
     * Gets all the headers from the response.
     */
    @Deprecated
    Map<String, List<String>> getAllHeaders();

    /**
     * Gets the underlying implementation response object, if any.
     */
    @Override
    Object getUnderlying();

    @Override
    String getContentType();

    /**
     * @return the HTTP status code from the response.
     */
    @Override
    int getStatus();

    /**
     * @return the text associated with the status code.
     */
    @Override
    String getStatusText();

    /**
     * @return all the cookies from the response.
     */
    @Override
    List<WSCookie> getCookies();

    /**
     * @return a single cookie from the response, if any.
     */
    @Override
    Optional<WSCookie> getCookie(String name);

    //----------------------------------
    // Body methods
    //----------------------------------

    /**
     * @return the body as a string.
     */
    @Override
    String getBody();

    /** @return the body as a ByteString */
    @Override
    ByteString getBodyAsBytes();

    /**
     * @return the body as a Source
     */
    @Override
    Source<ByteString, ?> getBodyAsSource();

    /**
     * Gets the body of the response as a T, using a {@link BodyReadable}.
     *
     * See {@link WSBodyReadables} for convenient functions.
     *
     * @param readable a transformation function from a response to a T.
     * @param <T> the type to return, i.e. String.
     * @return the body as an instance of T.
     */
    @Override
    <T> T getBody(BodyReadable<T> readable);

    /**
     * return the body as XML.
     */
    Document asXml();

    /**
     * Gets the body as JSON node.
     * @return json node.
     */
    JsonNode asJson();

    /**
     * Gets the body as a stream.
     *
     * @deprecated use {@link #getBody(BodyReadable)} with {@code WSBodyWritables.inputStream()}.
     */
    @Deprecated
    InputStream getBodyAsStream();

    /**
     * Gets the body as an array of bytes.
     */
    byte[] asByteArray();

}
