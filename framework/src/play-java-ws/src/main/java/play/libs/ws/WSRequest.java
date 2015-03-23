/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;


import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * This is the main interface to building a WS request in Java.
 * <p>
 * Note that this interface does not expose properties that are only exposed
 * after building the request: notably, the URL, headers and query parameters
 * are shown before an OAuth signature is calculated.
 */
public interface WSRequest {

    //-------------------------------------------------------------------------
    // "GET"
    //-------------------------------------------------------------------------

    /**
     * Perform a GET on the request asynchronously.
     */
    F.Promise<WSResponse> get();

    //-------------------------------------------------------------------------
    // "PATCH"
    //-------------------------------------------------------------------------

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as String
     */
    F.Promise<WSResponse> patch(String body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as JSON
     */
    F.Promise<WSResponse> patch(JsonNode body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as an InputStream
     */
    F.Promise<WSResponse> patch(InputStream body);

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as a File
     */
    F.Promise<WSResponse> patch(File body);

    //-------------------------------------------------------------------------
    // "POST"
    //-------------------------------------------------------------------------

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as String
     */
    F.Promise<WSResponse> post(String body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as JSON
     */
    F.Promise<WSResponse> post(JsonNode body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as an InputStream
     */
    F.Promise<WSResponse> post(InputStream body);

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as a File
     */
    F.Promise<WSResponse> post(File body);

    //-------------------------------------------------------------------------
    // "PUT"
    //-------------------------------------------------------------------------

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as String
     */
    F.Promise<WSResponse> put(String body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as JSON
     */
    F.Promise<WSResponse> put(JsonNode body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as an InputStream
     */
    F.Promise<WSResponse> put(InputStream body);

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as a File
     */
    F.Promise<WSResponse> put(File body);

    //-------------------------------------------------------------------------
    // Miscellaneous execution methods
    //-------------------------------------------------------------------------

    /**
     * Perform a DELETE on the request asynchronously.
     */
    F.Promise<WSResponse> delete();

    /**
     * Perform a HEAD on the request asynchronously.
     */
    F.Promise<WSResponse> head();

    /**
     * Perform an OPTIONS on the request asynchronously.
     */
    F.Promise<WSResponse> options();

    /**
     * Execute an arbitrary method on the request asynchronously.
     *
     * @param method The method to execute
     */
    F.Promise<WSResponse> execute(String method);

    /**
     * Execute an arbitrary method on the request asynchronously.  Should be used with setMethod().
     */
    F.Promise<WSResponse> execute();

    //-------------------------------------------------------------------------
    // Setters
    //-------------------------------------------------------------------------

    /**
     * Set the HTTP method this request should use, where the no args execute() method is invoked.
     */
    WSRequest setMethod(String method);

    /**
     * Set the body this request should use.
     */
    WSRequest setBody(String body);

    /**
     * Set the body this request should use.
     */
    WSRequest setBody(JsonNode body);

    /**
     * Set the body this request should use.
     */
    WSRequest setBody(InputStream body);

    /**
     * Set the body this request should use.
     */
    WSRequest setBody(File body);

    /**
     * Adds a header to the request.  Note that duplicate headers are allowed
     * by the HTTP specification, and removing a header is not available
     * through this API.
     */
    WSRequest setHeader(String name, String value);

    /**
     * Sets the query string to query.
     */
    WSRequest setQueryString(String query);

    /**
     * Sets a query parameter with the given name, this can be called repeatedly.  Duplicate query parameters are allowed.
     *
     * @param name
     * @param value
     */
    WSRequest setQueryParameter(String name, String value);

    /**
     * Sets the authentication header for the current request using BASIC authentication.
     *
     * @param userInfo
     */
    WSRequest setAuth(String userInfo);

    /**
     * Sets the authentication header for the current request using BASIC authentication.
     *
     * @param username
     * @param password
     */
    WSRequest setAuth(String username, String password);

    /**
     * Sets the authentication header for the current request.
     *
     * @param username
     * @param password
     * @param scheme   authentication scheme
     */
    WSRequest setAuth(String username, String password, WSAuthScheme scheme);

    /**
     * Sets an (OAuth) signature calculator.
     */
    WSRequest sign(WSSignatureCalculator calculator);

    /**
     * Sets whether redirects (301, 302) should be followed automatically.
     *
     * @param followRedirects
     */
    WSRequest setFollowRedirects(Boolean followRedirects);

    /**
     * Sets the virtual host as a "hostname:port" string.
     */
    WSRequest setVirtualHost(String virtualHost);

    /**
     * Sets the request timeout in milliseconds.
     *
     * @param timeout the request timeout in milliseconds.
     * @return the modified WSRequest.
     */
    WSRequest setRequestTimeout(long timeout);

    /**
     * Set the content type.  If the request body is a String, and no charset parameter is included, then it will
     * default to UTF-8.
     *
     * @param contentType The content type
     */
    WSRequest setContentType(String contentType);

    //-------------------------------------------------------------------------
    // Getters
    //-------------------------------------------------------------------------

    /**
     * @return the URL of the request.  This has not passed through an internal request builder and so will not be signed.
     */
    String getUrl();

    /**
     * @return the headers (a copy to prevent side-effects). This has not passed through an internal request builder and so will not be signed.
     */
    Map<String, Collection<String>> getHeaders();

    /**
     * @return the query parameters (a copy to prevent side-effects). This has not passed through an internal request builder and so will not be signed.
     */
    Map<String, Collection<String>> getQueryParameters();

    /**
     * @return the auth username, null if not an authenticated request.
     */
    String getUsername();

    /**
     * @return the auth password, null if not an authenticated request
     */
    String getPassword();

    /**
     * @return the auth scheme, null if not an authenticated request.
     */
    WSAuthScheme getScheme();

    /**
     * @return the signature calculator (example: OAuth), null if none is set.
     */
    WSSignatureCalculator getCalculator();

    /**
     * Gets the original request timeout in milliseconds, passed into the
     * request as input.
     */
    long getRequestTimeout();

    /**
     * @return true if the request is configure to follow redirect, false if it is configure not to, null if nothing is configured and the global client preference should be used instead.
     */
    Boolean getFollowRedirects();

}
