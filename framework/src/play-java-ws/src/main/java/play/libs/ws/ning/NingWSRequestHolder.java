/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package play.libs.ws.ning;

import com.fasterxml.jackson.databind.JsonNode;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.PerRequestConfig;
import com.ning.http.util.AsyncHttpProviderUtils;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.*;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;

/**
 * provides the User facing API for building WS request.
 */
public class NingWSRequestHolder implements WSRequestHolder {

    private final String url;
    private String method = "GET";
    private Object body = null;
    private Map<String, Collection<String>> headers = new HashMap<String, Collection<String>>();
    private Map<String, Collection<String>> queryParameters = new HashMap<String, Collection<String>>();

    private String username = null;
    private String password = null;
    private WSAuthScheme scheme = null;
    private WSSignatureCalculator calculator = null;
    private NingWSClient client = null;

    private int timeout = 0;
    private Boolean followRedirects = null;
    private String virtualHost = null;

    public NingWSRequestHolder(NingWSClient client, String url) {
        this.client = client;
        URI reference = URI.create(url);

        this.url = url;

        String userInfo = reference.getUserInfo();
        if (userInfo != null) {
            this.setAuth(userInfo);
        }
        if (reference.getQuery() != null) {
            this.setQueryString(reference.getQuery());
        }
    }

    /**
     * Sets a header with the given name, this can be called repeatedly.
     *
     * @param name
     * @param value
     */
    @Override
    public NingWSRequestHolder setHeader(String name, String value) {
        if (headers.containsKey(name)) {
            Collection<String> values = headers.get(name);
            values.add(value);
        } else {
            List<String> values = new ArrayList<String>();
            values.add(value);
            headers.put(name, values);
        }
        return this;
    }

    /**
     * Sets a query string
     *
     * @param query
     */
    @Override
    public WSRequestHolder setQueryString(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length > 2) {
                throw new RuntimeException(new MalformedURLException("QueryString parameter should not have more than 2 = per part"));
            } else if (keyValue.length >= 2) {
                this.setQueryParameter(keyValue[0], keyValue[1]);
            } else if (keyValue.length == 1 && param.charAt(0) != '=') {
                this.setQueryParameter(keyValue[0], null);
            } else {
                throw new RuntimeException(new MalformedURLException("QueryString part should not start with an = and not be empty"));
            }
        }
        return this;
    }

    /**
     * Sets a query parameter with the given name,this can be called repeatedly.
     *
     * @param name
     * @param value
     */
    @Override
    public WSRequestHolder setQueryParameter(String name, String value) {
        if (queryParameters.containsKey(name)) {
            Collection<String> values = queryParameters.get(name);
            values.add(value);
        } else {
            List<String> values = new ArrayList<String>();
            values.add(value);
            queryParameters.put(name, values);
        }
        return this;
    }

    /**
     * Sets the authentication header for the current request using BASIC authentication.
     *
     * @param userInfo
     */
    @Override
    public WSRequestHolder setAuth(String userInfo) {
        this.scheme = WSAuthScheme.BASIC;

        if (userInfo.equals("")) {
            throw new RuntimeException(new MalformedURLException("userInfo should not be empty"));
        }

        int split = userInfo.indexOf(":");

        if (split == 0) { // We only have a password without user
            this.username = "";
            this.password = userInfo.substring(1);
        } else if (split == -1) { // We only have a username without password
            this.username = userInfo;
            this.password = "";
        } else {
            this.username = userInfo.substring(0, split);
            this.password = userInfo.substring(split + 1);
        }

        return this;
    }

    /**
     * Sets the authentication header for the current request using BASIC authentication.
     *
     * @param username
     * @param password
     */
    @Override
    public WSRequestHolder setAuth(String username, String password) {
        this.username = username;
        this.password = password;
        this.scheme = WSAuthScheme.BASIC;
        return this;
    }

    /**
     * Sets the authentication header for the current request.
     *
     * @param username
     * @param password
     * @param scheme   authentication scheme
     */
    @Override
    public WSRequestHolder setAuth(String username, String password, WSAuthScheme scheme) {
        this.username = username;
        this.password = password;
        this.scheme = scheme;
        return this;
    }

    @Override
    public WSRequestHolder sign(WSSignatureCalculator calculator) {
        this.calculator = calculator;
        return this;
    }

    /**
     * Sets whether redirects (301, 302) should be followed automatically.
     *
     * @param followRedirects
     */
    @Override
    public WSRequestHolder setFollowRedirects(Boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    /**
     * Sets the virtual host.
     */
    @Override
    public WSRequestHolder setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
        return this;
    }

    /**
     * Sets the request timeout in milliseconds.
     *
     * @param timeout
     */
    @Override
    public WSRequestHolder setTimeout(int timeout) {
        return setRequestTimeout(timeout);
    }

    /**
     * Sets the request timeout in milliseconds.
     *
     * @param timeout
     */
    @Override
    public WSRequestHolder setRequestTimeout(long timeout) {
        if (timeout < 0 || timeout > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout must be between 0 and " + Integer.MAX_VALUE + " inclusive");
        }
        this.timeout = (int)timeout;
        return this;
    }

    /**
     * Set the content type.  If the request body is a String, and no charset parameter is included, then it will
     * default to UTF-8.
     *
     * @param contentType The content type
     */
    @Override
    public WSRequestHolder setContentType(String contentType) {
        return setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType);
    }

    @Override
    public WSRequestHolder setMethod(String method) {
        this.method = method;
        return this;
    }

    @Override
    public WSRequestHolder setBody(String body) {
        this.body = body;
        return this;
    }

    @Override
    public WSRequestHolder setBody(JsonNode body) {
        this.body = body;
        return this;
    }

    @Override
    public WSRequestHolder setBody(InputStream body) {
        this.body = body;
        return this;
    }

    @Override
    public WSRequestHolder setBody(File body) {
        this.body = body;
        return this;
    }

    /**
     * @return the URL of the request.
     */
    @Override
    public String getUrl() {
        return this.url;
    }

    /**
     * @return the headers (a copy to prevent side-effects).
     */
    @Override
    public Map<String, Collection<String>> getHeaders() {
        return new HashMap<String, Collection<String>>(this.headers);
    }

    /**
     * @return the query parameters (a copy to prevent side-effects).
     */
    @Override
    public Map<String, Collection<String>> getQueryParameters() {
        return new HashMap<String, Collection<String>>(this.queryParameters);
    }

    /**
     * @return the auth username, null if not an authenticated request.
     */
    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * @return the auth password, null if not an authenticated request
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * @return the auth scheme, null if not an authenticated request
     */
    @Override
    public WSAuthScheme getScheme() {
        return this.scheme;
    }

    /**
     * @return the signature calculator (exemple: OAuth), null if none is set.
     */
    @Override
    public WSSignatureCalculator getCalculator() {
        return this.calculator;
    }

    /**
     * @return the request timeout.
     */
    @Override
    public int getTimeout() {
        return (int)getRequestTimeout();
    }

    /**
     * @return the request timeout.
     */
    @Override
    public long getRequestTimeout() {
        return this.timeout;
    }

    /**
     * @return true if the request is configure to follow redirect, false if it is configure not to, null if nothing is configured and the global client preference should be used instead.
     */
    @Override
    public Boolean getFollowRedirects() {
        return this.followRedirects;
    }

    // Intentionally package public.
    String getVirtualHost() {
        return this.virtualHost;
    }

    /**
     * Perform a GET on the request asynchronously.
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> get() {
        return execute("GET");
    }

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as String
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> patch(String body) {
        setMethod("PATCH");
        return executeString(body);
    }

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as String
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> post(String body) {
        setMethod("POST");
        return executeString(body);
    }

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as String
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> put(String body) {
        setMethod("PUT");
        return executeString(body);
    }

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as JSON
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> patch(JsonNode body) {
        setMethod("PATCH");
        return executeJson(body);
    }

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as JSON
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> post(JsonNode body) {
        setMethod("POST");
        return executeJson(body);
    }

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as JSON
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> put(JsonNode body) {
        setMethod("PUT");
        return executeJson(body);
    }

    /**
     * Perform a PATCH on the request asynchronously.
     *
     * @param body represented as an InputStream
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> patch(InputStream body) {
        setMethod("PATCH");
        return executeIS(body);
    }

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as an InputStream
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> post(InputStream body) {
        setMethod("POST");
        return executeIS(body);
    }

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as an InputStream
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> put(InputStream body) {
        setMethod("PUT");
        return executeIS(body);
    }

    /**
     * Perform a POST on the request asynchronously.
     *
     * @param body represented as a File
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> post(File body) {
        setMethod("POST");
        return executeFile(body);
    }

    /**
     * Perform a PUT on the request asynchronously.
     *
     * @param body represented as a File
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> put(File body) {
        setMethod("PUT");
        return executeFile(body);
    }

    /**
     * Perform a DELETE on the request asynchronously.
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> delete() {
        return execute("DELETE");
    }

    /**
     * Perform a HEAD on the request asynchronously.
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> head() {
        return execute("HEAD");
    }

    /**
     * Perform an OPTIONS on the request asynchronously.
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> options() {
        return execute("OPTIONS");
    }

    /**
     * Execute an arbitrary method on the request asynchronously.
     *
     * @param method The method to execute
     */
    @Override
    public F.Promise<play.libs.ws.WSResponse> execute(String method) {
        setMethod(method);
        return execute();
    }

    @Override
    public F.Promise<WSResponse> execute() {
        if (body == null) {
            NingWSRequest req = new NingWSRequest(client, method, url, queryParameters, headers);
            return execute(req);
        } else if (body instanceof String) {
            return executeString((String) body);
        } else if (body instanceof JsonNode) {
            return executeJson((JsonNode) body);
        } else if (body instanceof File) {
            return executeFile((File) body);
        } else if (body instanceof InputStream) {
            return executeIS((InputStream) body);
        } else {
            throw new IllegalStateException("Impossible body: " + body);
        }
    }

    private F.Promise<play.libs.ws.WSResponse> executeString(String body) {
        FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap(this.headers);

        // Detect and maybe add charset
        String contentType = headers.getFirstValue(HttpHeaders.Names.CONTENT_TYPE);
        if (contentType == null) {
            contentType = "text/plain";
        }
        String charset = AsyncHttpProviderUtils.parseCharset(contentType);
        if (charset == null) {
            charset = "utf-8";
            headers.replace(HttpHeaders.Names.CONTENT_TYPE, contentType + "; charset=utf-8");
        }

        byte[] bodyBytes;
        try {
            bodyBytes = body.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        NingWSRequest req = new NingWSRequest(client, method, url, queryParameters, headers, bodyBytes)
                .setBody(body)
                .setBodyEncoding(charset);
        return execute(req);
    }

    private F.Promise<play.libs.ws.WSResponse> executeJson(JsonNode body) {
        FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap(this.headers);
        headers.replace(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8");
        String bodyStr = Json.stringify(body);
        byte[] bodyBytes;
        try {
            bodyBytes = bodyStr.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        NingWSRequest req = new NingWSRequest(client, method, url, queryParameters, headers, bodyBytes)
                .setBody(bodyStr)
                .setBodyEncoding("utf-8");
        return execute(req);

    }

    private F.Promise<play.libs.ws.WSResponse> executeIS(InputStream body) {
        NingWSRequest req = new NingWSRequest(client, method, url, queryParameters, headers)
                .setBody(body);
        return execute(req);
    }

    private F.Promise<play.libs.ws.WSResponse> executeFile(File body) {
        NingWSRequest req = new NingWSRequest(client, method, url, queryParameters, headers)
                .setBody(body);
        return execute(req);
    }

    private F.Promise<WSResponse> execute(NingWSRequest req) {
        if (this.timeout > 0) {
            PerRequestConfig config = new PerRequestConfig();
            config.setRequestTimeoutInMs(this.timeout);
            req.setPerRequestConfig(config);
        }
        if (this.followRedirects != null) {
            req.setFollowRedirects(this.followRedirects);
        }
        if (this.virtualHost != null) {
            req.setVirtualHost(this.virtualHost);
        }
        if (this.username != null && this.password != null && this.scheme != null)
            req.auth(this.username, this.password, this.scheme);
        if (this.calculator != null)
            this.calculator.sign(req);

        return req.execute();
    }
}
