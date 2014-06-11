/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws.ning;

import com.ning.http.client.*;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import play.libs.F;
import play.libs.ws.WSAuthScheme;
import play.libs.ws.WSRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides the bridge between Play and the underlying ning request
 */
public class NingWSRequest implements WSRequest {

    private final FluentCaseInsensitiveStringsMap headers;
    private final String method;
    private final RequestBuilder builder;
    private final NingWSClient client;
    private final byte[] body;

    public NingWSRequest(NingWSClient client, String method, String url, Map<String, Collection<String>> queryString,
                         Map<String, Collection<String>> headers) {
        this(client, method, url, queryString, new FluentCaseInsensitiveStringsMap(headers), null);
    }

    public NingWSRequest(NingWSClient client, String method, String url, Map<String, Collection<String>> queryString,
                         FluentCaseInsensitiveStringsMap headers) {
        this(client, method, url, queryString, headers, null);
    }

    public NingWSRequest(NingWSClient client, String method, String url, Map<String, Collection<String>> queryString,
                         FluentCaseInsensitiveStringsMap headers, byte[] body) {
        this.client = client;
        this.builder = new RequestBuilder(method);
        this.method = method;
        this.headers = headers;
        this.body = body;
        builder.setUrl(url)
                .setQueryParameters(new FluentStringsMap(queryString))
                .setHeaders(headers);
    }

    /**
     * Return the headers of the request being constructed
     */
    @Override
    public Map<String, List<String>> getAllHeaders() {
        return headers;
    }

    @Override
    public List<String> getHeader(String name) {
        List<String> hdrs = headers.get(name);
        if (hdrs == null) return new ArrayList<String>();
        return hdrs;
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public String getUrl() {
        return builder.build().getUrl();
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public WSRequest auth(String username, String password, WSAuthScheme scheme) {
        Realm.AuthScheme authScheme = getAuthScheme(scheme);
        builder.setRealm((new Realm.RealmBuilder())
                .setScheme(authScheme)
                .setPrincipal(username)
                .setPassword(password)
                .setUsePreemptiveAuth(true)
                .build());
        return this;
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> execute() {
        final scala.concurrent.Promise<play.libs.ws.WSResponse> scalaPromise = scala.concurrent.Promise$.MODULE$.<play.libs.ws.WSResponse>apply();
        try {
            AsyncHttpClient asyncHttpClient = (AsyncHttpClient) client.getUnderlying();
            asyncHttpClient.executeRequest(getBuilder().build(), new AsyncCompletionHandler<com.ning.http.client.Response>() {
                @Override
                public com.ning.http.client.Response onCompleted(com.ning.http.client.Response response) {
                    final com.ning.http.client.Response ahcResponse = response;
                    scalaPromise.success(new NingWSResponse(ahcResponse));
                    return response;
                }
                @Override
                public void onThrowable(Throwable t) {
                    scalaPromise.failure(t);
                }
            });
        } catch (IOException exception) {
            scalaPromise.failure(exception);
        }
        return F.Promise.wrap(scalaPromise.future());
    }

    /**
     * Set an HTTP header.
     */
    @Deprecated
    public NingWSRequest setHeader(String name, String value) {
        builder.setHeader(name, value);
        return this;
    }

    /**
     * Add an HTTP header (used for headers with mutiple values).
     */
    @Deprecated
    public NingWSRequest addHeader(String name, String value) {
        if (value == null) {
            value = "";
        }
        builder.addHeader(name, value);
        return this;
    }

    /**
     * Defines the request headers.
     */
    @Deprecated
    public NingWSRequest setHeaders(Map<String, Collection<String>> hdrs) {
        builder.setHeaders(hdrs);
        return this;
    }

    @Deprecated
    public NingWSRequest setUrl(String url) {
        builder.setUrl(url);
        return this;
    }

    NingWSRequest setBody(String body) {
        builder.setBody(body);
        return this;
    }

    NingWSRequest setBodyEncoding(String charset) {
        builder.setBodyEncoding(charset);
        return this;
    }

    NingWSRequest setBody(InputStream body) {
        builder.setBody(new InputStreamBodyGenerator(body));
        return this;
    }

    NingWSRequest setPerRequestConfig(PerRequestConfig config) {
        builder.setPerRequestConfig(config);
        return this;
    }

    NingWSRequest setFollowRedirects(Boolean followRedirects) {
        builder.setFollowRedirects(followRedirects);
        return this;
    }

    NingWSRequest setBody(File body) {
        builder.setBody(body);
        return this;
    }

    // intentionally package private.
    NingWSRequest setVirtualHost(String virtualHost) {
        builder.setVirtualHost(virtualHost);
        return this;
    }

    RequestBuilder getBuilder() {
        return builder;
    }


    Realm.AuthScheme getAuthScheme(WSAuthScheme scheme) {
        return Realm.AuthScheme.valueOf(scheme.name());
    }

}
