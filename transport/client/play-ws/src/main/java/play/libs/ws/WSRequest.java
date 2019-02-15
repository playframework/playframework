/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;
import play.mvc.Http;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * This is the main interface to building a WS request in Java.
 * <p>
 * Note that this interface does not expose properties that are only exposed
 * after building the request: notably, the URL, headers and query parameters
 * are shown before an OAuth signature is calculated.
 */
public interface WSRequest extends StandaloneWSRequest {

    //-------------------------------------------------------------------------
    // "GET"
    //-------------------------------------------------------------------------

    /**
     * Perform a GET on the request asynchronously.
     *
     * @return a promise to the response
     */
    @Override
    CompletionStage<WSResponse> get();

    //-------------------------------------------------------------------------
    // "PATCH"
    //-------------------------------------------------------------------------

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as BodyWritable
     * @return a promise to the response
     */
    @Override
    CompletionStage<WSResponse> patch(BodyWritable body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as String
     * @return a promise to the response
     */
    CompletionStage<WSResponse> patch(String body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as JSON
     * @return a promise to the response
     */
    CompletionStage<WSResponse> patch(JsonNode body);

    /**
     *  Perform a PATCH on the request asynchronously.
     *
     * @param body represented as a Document
     * @return a promise to the response
     */
    CompletionStage<WSResponse> patch(Document body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as an InputStream
     * @return a promise to the response
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #patch(BodyWritable)} instead.
     */
    @Deprecated
    CompletionStage<WSResponse> patch(InputStream body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as a File
     * @return a promise to the response
     */
    CompletionStage<WSResponse> patch(File body);

    /**
     * Perform a PATCH on the request asynchronously.
     * @param body represented as a MultipartFormData.Part
     * @return a promise to the response
     */
    CompletionStage<WSResponse> patch(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body);

    //-------------------------------------------------------------------------
    // "POST"
    //-------------------------------------------------------------------------

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as body writable
     * @return a promise to the response
     */
    @Override
    CompletionStage<WSResponse> post(BodyWritable body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as String
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(String body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as JSON
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(JsonNode body);

    /**
     *  Perform a POST on the request asynchronously.
     *
     * @param body represented as a Document
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(Document body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as an InputStream
     * @return a promise to the response
     *
     * @deprecated Deprecated as of 2.6.0. Use {@link #post(BodyWritable)} instead.
     */
    @Deprecated
    CompletionStage<WSResponse> post(InputStream body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as a File
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(File body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as a MultipartFormData.Part
     * @return a promise to the response
     */
    CompletionStage<WSResponse> post(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body);

    //-------------------------------------------------------------------------
    // "PUT"
    //-------------------------------------------------------------------------

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as BodyWritable
     * @return a promise to the response
     */
    @Override
    CompletionStage<WSResponse> put(BodyWritable body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as String
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(String body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as JSON
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(JsonNode body);

    /**
     *  Perform a PUT on the request asynchronously.
     *
     * @param body represented as a Document
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(Document body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as an InputStream
     * @return a promise to the response
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #put(BodyWritable)} instead.
     */
    @Deprecated
    CompletionStage<WSResponse> put(InputStream body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as a File
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(File body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as a MultipartFormData.Part
     * @return a promise to the response
     */
    CompletionStage<WSResponse> put(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body);

    //-------------------------------------------------------------------------
    // Miscellaneous execution methods
    //-------------------------------------------------------------------------

    /**
     * Perform a DELETE on the request asynchronously.
     *
     * @return a promise to the response
     */
    @Override
    CompletionStage<WSResponse> delete();

    /**
     * Perform a HEAD on the request asynchronously.
     *
     * @return a promise to the response
     */
    @Override
    CompletionStage<WSResponse> head();

    /**
     * Perform an OPTIONS on the request asynchronously.
     *
     * @return a promise to the response
     */
    @Override
    CompletionStage<WSResponse> options();

    /**
     * Execute an arbitrary method on the request asynchronously.
     *
     * @param method The method to execute
     * @return a promise to the response
     */
    @Override
    CompletionStage<WSResponse> execute(String method);

    /**
     * Execute an arbitrary method on the request asynchronously.  Should be used with setMethod().
     *
     * @return a promise to the response
     */
    @Override
    CompletionStage<WSResponse> execute();

    /**
     * Execute this request and stream the response body.
     *
     * @return a promise to the streaming response
     */
    @Override
    CompletionStage<WSResponse> stream();

    //-------------------------------------------------------------------------
    // Setters
    //-------------------------------------------------------------------------

    /**
     * Sets the HTTP method this request should use, where the no args execute() method is invoked.
     *
     * @param method the HTTP method.
     * @return the modified WSRequest.
     */
    @Override
    WSRequest setMethod(String method);

    /**
     * Set the body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    @Override
    WSRequest setBody(BodyWritable body);

    /**
     * Set the body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    WSRequest setBody(String body);

    /**
     * Set the body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    WSRequest setBody(JsonNode body);

    /**
     * Set the body this request should use.
     *
     * @param body the request body.
     * @return the modified WSRequest.
     *
     * @deprecated Deprecated as of 2.6.0. Use {@link #setBody(BodyWritable)} instead.
     */
    @Deprecated
    WSRequest setBody(InputStream body);

    /**
     * Set the body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    WSRequest setBody(File body);

    /**
     * Set the body this request should use.
     *
     * @param body the body of the request.
     * @return the modified WSRequest.
     */
    <U> WSRequest setBody(Source<ByteString, U> body);

    /**
     * Adds a header to the request.  Note that duplicate headers are allowed
     * by the HTTP specification, and removing a header is not available
     * through this API.
     *
     * @param name  the header name
     * @param value the header value
     * @return the modified WSRequest.
     */
    @Override
    WSRequest addHeader(String name, String value);

    /**
     * Adds a header to the request.  Note that duplicate headers are allowed
     * by the HTTP specification, and removing a header is not available
     * through this API.
     *
     * @deprecated use {@link #addHeader(String, String)}
     * @param name  the header name
     * @param value the header value
     * @return the modified WSRequest.
     */
    @Deprecated
    WSRequest setHeader(String name, String value);

    /**
     * Sets all of the headers on the request.
     *
     * @param headers the headers
     * @return the modified WSRequest.
     */
    @Override
    WSRequest setHeaders(Map<String, List<String>> headers);

    /**
     * Sets the query string to query.
     *
     * @param query the fully formed query string
     * @return the modified WSRequest.
     */
    @Override
    WSRequest setQueryString(String query);

    /**
     * Sets the query string to query.
     *
     * @param params the query string parameters
     * @return the modified WSRequest.
     */
    @Override
    WSRequest setQueryString(Map<String, List<String>> params);

    /**
     * Sets a query parameter with the given name, this can be called repeatedly.  Duplicate query parameters are allowed.
     *
     * @param name  the query parameter name
     * @param value the query parameter value
     * @return the modified WSRequest.
     */
    @Override
    WSRequest addQueryParameter(String name, String value);

    /**
     * Sets a query parameter with the given name, this can be called repeatedly.  Duplicate query parameters are allowed.
     *
     * @deprecated use {@link #addQueryParameter(String, String)}
     * @param name  the query parameter name
     * @param value the query parameter value
     * @return the modified WSRequest.
     */
    @Deprecated
    WSRequest setQueryParameter(String name, String value);

    /**
     * Adds a cookie to the request
     *
     * @param cookie the cookie to add.
     * @return the modified request
     */
    @Override
    WSRequest addCookie(WSCookie cookie);

    /**
     * Adds a cookie to the request
     *
     * @param cookie the cookie to add.
     * @return the modified request
     */
    WSRequest addCookie(Http.Cookie cookie);

    /**
     * Sets several cookies on the request.
     *
     * @param cookies the cookies.
     * @return the modified request
     */
    @Override
    WSRequest addCookies(WSCookie... cookies);

    /**
     * Sets all the cookies on the request.
     *
     * @param cookies all the cookies.
     * @return the modified request
     */
    @Override
    WSRequest setCookies(List<WSCookie> cookies);

    /**
     * Sets the authentication header for the current request using BASIC authentication.
     *
     * @param userInfo a string formed as "username:password".
     * @return the modified WSRequest.
     */
    @Override
    WSRequest setAuth(String userInfo);

    /**
     * Sets the authentication header for the current request using BASIC authentication.
     *
     * @param username the basic auth username
     * @param password the basic auth password
     * @return the modified WSRequest.
     */
    @Override
    WSRequest setAuth(String username, String password);

    /**
     * Sets the authentication header for the current request.
     *
     * @param username the username
     * @param password the password
     * @param scheme   authentication scheme
     * @return the modified WSRequest.
     */
    @Override
    WSRequest setAuth(String username, String password, WSAuthScheme scheme);

    /**
     * Sets an (OAuth) signature calculator.
     *
     * @param calculator the signature calculator
     * @return the modified WSRequest
     */
    @Override
    WSRequest sign(WSSignatureCalculator calculator);

    /**
     * Sets whether redirects (301, 302) should be followed automatically.
     *
     * @param followRedirects true if the request should follow redirects
     * @return the modified WSRequest
     */
    @Override
    WSRequest setFollowRedirects(boolean followRedirects);

    /**
     * Sets the virtual host as a "hostname:port" string.
     *
     * @param virtualHost the virtual host
     * @return the modified WSRequest
     */
    @Override
    WSRequest setVirtualHost(String virtualHost);

    /**
     * Sets the request timeout in milliseconds.
     *
     * @param timeout the request timeout in milliseconds. A value of -1 indicates an infinite request timeout.
     * @return the modified WSRequest.
     */
    @Override
    WSRequest setRequestTimeout(Duration timeout);

    /**
     * Sets the request timeout in milliseconds.
     *
     * @deprecated use {@link #setRequestTimeout(Duration)}
     * @param timeout the request timeout in milliseconds. A value of -1 indicates an infinite request timeout.
     * @return the modified WSRequest.
     */
    @Deprecated
    WSRequest setRequestTimeout(long timeout);

    /**
     * Adds a request filter.
     *
     * @param filter a transforming filter.
     * @return the modified request.
     */
    @Override
    WSRequest setRequestFilter(WSRequestFilter filter);

    /**
     * Set the content type.  If the request body is a String, and no charset parameter is included, then it will
     * default to UTF-8.
     *
     * @param contentType The content type
     * @return the modified WSRequest
     */
    @Override
    WSRequest setContentType(String contentType);

    //-------------------------------------------------------------------------
    // Getters
    //-------------------------------------------------------------------------

    /**
     * @return the URL of the request.  This has not passed through an internal request builder and so will not be signed.
     */
    @Override
    String getUrl();

    /**
     * @return the headers (a copy to prevent side-effects). This has not passed through an internal request builder and so will not be signed.
     */
    @Override
    Map<String, List<String>> getHeaders();

    /**
     * @return the query parameters (a copy to prevent side-effects). This has not passed through an internal request builder and so will not be signed.
     */
    @Override
    Map<String, List<String>> getQueryParameters();
}
