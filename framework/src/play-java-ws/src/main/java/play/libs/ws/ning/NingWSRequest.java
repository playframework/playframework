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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides the bridge between Play and the underlying ning request
 */
public class NingWSRequest implements WSRequest {

    private FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();

    private String method;

    private String url;

    private RequestBuilder builder;

    private NingWSClient client;

    public NingWSRequest(NingWSClient client, String method) {
        this.client = client;
        this.builder = new RequestBuilder(method);
        this.method = method;
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
        return this.url;
    }

    @Override
    public WSRequest auth(String username, String password, WSAuthScheme scheme) {
        Realm.AuthScheme authScheme = getAuthScheme(scheme);
        return setBuilder(getBuilder().setRealm((new Realm.RealmBuilder())
                .setScheme(authScheme)
                .setPrincipal(username)
                .setPassword(password)
                .setUsePreemptiveAuth(true)
                .build()));
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
        return new F.Promise<play.libs.ws.WSResponse>(scalaPromise.future());
    }

    /**
     * Set an HTTP header.
     */
    @Deprecated
    public NingWSRequest setHeader(String name, String value) {
        headers.replace(name, value);
        return setBuilder(getBuilder().setHeader(name, value));
    }

    /**
     * Add an HTTP header (used for headers with mutiple values).
     */
    @Deprecated
    public NingWSRequest addHeader(String name, String value) {
        if (value == null) {
            value = "";
        }
        headers.add(name, value);
        return setBuilder(getBuilder().addHeader(name, value));
    }

    /**
     * Defines the request headers.
     */
    @Deprecated
    public NingWSRequest setHeaders(FluentCaseInsensitiveStringsMap hdrs) {
        headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : headers);
        return setBuilder(getBuilder().setHeaders(hdrs));
    }

    /**
     * Defines the request headers.
     */
    @Deprecated
    public NingWSRequest setHeaders(Map<String, Collection<String>> hdrs) {
        headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : new FluentCaseInsensitiveStringsMap(headers));
        return setBuilder(getBuilder().setHeaders(hdrs));
    }

    @Deprecated
    public NingWSRequest setUrl(String url) {
        this.url = url;
        return setBuilder(getBuilder().setUrl(url));
    }

    @Deprecated
    public NingWSRequest setQueryParameters(FluentStringsMap entries) {
        return setBuilder(getBuilder().setQueryParameters(entries));
    }

    @Deprecated
    public NingWSRequest setBody(String body) {
        return setBuilder(getBuilder().setBody(body));
    }

    @Deprecated
    public NingWSRequest setBodyEncoding(String charset) {
        return setBuilder(getBuilder().setBodyEncoding(charset));
    }

    @Deprecated
    public NingWSRequest setBody(InputStream body) {
        return setBuilder(getBuilder().setBody(new InputStreamBodyGenerator(body)));
    }

    @Deprecated
    public NingWSRequest setPerRequestConfig(PerRequestConfig config) {
        return setBuilder(getBuilder().setPerRequestConfig(config));
    }

    @Deprecated
    public NingWSRequest setFollowRedirects(Boolean followRedirects) {
        return setBuilder(getBuilder().setFollowRedirects(followRedirects));
    }

    @Deprecated
    public NingWSRequest setBody(File body) {
        return setBuilder(getBuilder().setBody(body));
    }

    public RequestBuilder getBuilder() {
        return builder;
    }

    public NingWSRequest setBuilder(RequestBuilder builder) {
        this.builder = builder;
        return this;
    }

    public Realm.AuthScheme getAuthScheme(WSAuthScheme scheme) {
        return Realm.AuthScheme.valueOf(scheme.name());
    }

}
