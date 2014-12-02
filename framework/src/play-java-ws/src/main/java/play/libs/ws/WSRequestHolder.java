/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;


import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public interface WSRequestHolder {

    String getUsername();

    String getPassword();

    WSAuthScheme getScheme();

    WSSignatureCalculator getCalculator();

    @Deprecated
    int getTimeout();

    long getRequestTimeout();

    Boolean getFollowRedirects();

    F.Promise<WSResponse> get();

    F.Promise<WSResponse> patch(String body);

    F.Promise<WSResponse> post(String body);

    F.Promise<WSResponse> put(String body);

    F.Promise<WSResponse> patch(JsonNode body);

    F.Promise<WSResponse> post(JsonNode body);

    F.Promise<WSResponse> put(JsonNode body);

    F.Promise<WSResponse> patch(InputStream body);

    F.Promise<WSResponse> post(InputStream body);

    F.Promise<WSResponse> put(InputStream body);

    F.Promise<WSResponse> post(File body);

    F.Promise<WSResponse> put(File body);

    F.Promise<WSResponse> delete();

    F.Promise<WSResponse> head();

    F.Promise<WSResponse> options();

    F.Promise<WSResponse> execute(String method);

    /**
     * Execute the request
     */
    F.Promise<WSResponse> execute();

    /**
     * Set the method this request should use.
     */
    WSRequestHolder setMethod(String method);

    /**
     * Set the body this request should use
     */
    WSRequestHolder setBody(String body);

    /**
     * Set the body this request should use
     */
    WSRequestHolder setBody(JsonNode body);

    /**
     * Set the body this request should use
     */
    WSRequestHolder setBody(InputStream body);

    /**
     * Set the body this request should use
     */
    WSRequestHolder setBody(File body);

    WSRequestHolder setHeader(String name, String value);

    WSRequestHolder setQueryString(String query);

    WSRequestHolder setQueryParameter(String name, String value);

    WSRequestHolder setAuth(String userInfo);

    WSRequestHolder setAuth(String username, String password);

    WSRequestHolder setAuth(String username, String password, WSAuthScheme scheme);

    WSRequestHolder sign(WSSignatureCalculator calculator);

    WSRequestHolder setFollowRedirects(Boolean followRedirects);

    WSRequestHolder setVirtualHost(String virtualHost);

    @Deprecated
    WSRequestHolder setTimeout(int timeout);

    WSRequestHolder setRequestTimeout(long timeout);

    WSRequestHolder setContentType(String contentType);

    String getUrl();

    Map<String, Collection<String>> getHeaders();

    Map<String, Collection<String>> getQueryParameters();
}
