/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package play.libs.ws.ning;

import com.fasterxml.jackson.databind.JsonNode;
import com.ning.http.client.*;
import com.ning.http.client.generators.FileBodyGenerator;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.ning.http.client.oauth.OAuthSignatureCalculator;
import com.ning.http.util.AsyncHttpProviderUtils;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import play.core.parsers.FormUrlEncodedParser;
import play.libs.F;
import play.libs.Json;
import play.libs.oauth.OAuth;
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
public class NingWSRequest implements WSRequest {

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

    public NingWSRequest(NingWSClient client, String url) {
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
    public NingWSRequest setHeader(String name, String value) {
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
    public WSRequest setQueryString(String query) {
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

    @Override
    public WSRequest setQueryParameter(String name, String value) {
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

    @Override
    public WSRequest setAuth(String userInfo) {
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

    @Override
    public WSRequest setAuth(String username, String password) {
        this.username = username;
        this.password = password;
        this.scheme = WSAuthScheme.BASIC;
        return this;
    }

    @Override
    public WSRequest setAuth(String username, String password, WSAuthScheme scheme) {
        this.username = username;
        this.password = password;
        this.scheme = scheme;
        return this;
    }

    @Override
    public WSRequest sign(WSSignatureCalculator calculator) {
        this.calculator = calculator;
        return this;
    }

    @Override
    public WSRequest setFollowRedirects(Boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    @Override
    public WSRequest setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
        return this;
    }

    @Override
    public WSRequest setRequestTimeout(long timeout) {
        if (timeout < 0 || timeout > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout must be between 0 and " + Integer.MAX_VALUE + " inclusive");
        }
        this.timeout = (int) timeout;
        return this;
    }

    @Override
    public WSRequest setContentType(String contentType) {
        return setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType);
    }

    @Override
    public WSRequest setMethod(String method) {
        this.method = method;
        return this;
    }

    @Override
    public WSRequest setBody(String body) {
        this.body = body;
        return this;
    }

    @Override
    public WSRequest setBody(JsonNode body) {
        this.body = body;
        return this;
    }

    @Override
    public WSRequest setBody(InputStream body) {
        this.body = body;
        return this;
    }

    @Override
    public WSRequest setBody(File body) {
        this.body = body;
        return this;
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    @Override
    public Map<String, Collection<String>> getHeaders() {
        return new HashMap<String, Collection<String>>(this.headers);
    }

    @Override
    public Map<String, Collection<String>> getQueryParameters() {
        return new HashMap<String, Collection<String>>(this.queryParameters);
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public WSAuthScheme getScheme() {
        return this.scheme;
    }

    @Override
    public WSSignatureCalculator getCalculator() {
        return this.calculator;
    }

    @Override
    public long getRequestTimeout() {
        return this.timeout;
    }

    @Override
    public Boolean getFollowRedirects() {
        return this.followRedirects;
    }

    // Intentionally package public.
    String getVirtualHost() {
        return this.virtualHost;
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> get() {
        return execute("GET");
    }

    //-------------------------------------------------------------------------
    // PATCH
    //-------------------------------------------------------------------------

    @Override
    public F.Promise<play.libs.ws.WSResponse> patch(String body) {
        setMethod("PATCH");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> patch(JsonNode body) {
        setMethod("PATCH");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> patch(InputStream body) {
        setMethod("PATCH");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<WSResponse> patch(File body) {
        setMethod("PATCH");
        setBody(body);
        return execute();
    }

    //-------------------------------------------------------------------------
    // POST
    //-------------------------------------------------------------------------

    @Override
    public F.Promise<play.libs.ws.WSResponse> post(String body) {
        setMethod("POST");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> post(JsonNode body) {
        setMethod("POST");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> post(InputStream body) {
        setMethod("POST");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> post(File body) {
        setMethod("POST");
        setBody(body);
        return execute();
    }

    //-------------------------------------------------------------------------
    // PUT
    //-------------------------------------------------------------------------

    @Override
    public F.Promise<play.libs.ws.WSResponse> put(String body) {
        setMethod("PUT");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> put(JsonNode body) {
        setMethod("PUT");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> put(InputStream body) {
        setMethod("PUT");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> put(File body) {
        setMethod("PUT");
        setBody(body);
        return execute();
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> delete() {
        return execute("DELETE");
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> head() {
        return execute("HEAD");
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> options() {
        return execute("OPTIONS");
    }

    @Override
    public F.Promise<play.libs.ws.WSResponse> execute(String method) {
        setMethod(method);
        return execute();
    }

    @Override
    public F.Promise<WSResponse> execute() {
        Request request = buildRequest();
        return execute(request);
    }

    Request buildRequest() {
        RequestBuilder builder = new RequestBuilder(method);

        builder.setUrl(url);
        builder.setQueryParams(new FluentStringsMap(queryParameters));
        builder.setHeaders(headers);

        if (body == null) {
            // do nothing
        } else if (body instanceof String) {
            String stringBody = ((String) body);
            FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap(this.headers);

            // Detect and maybe add charset
            String contentType = headers.getFirstValue(HttpHeaders.Names.CONTENT_TYPE);
            if (contentType == null) {
                contentType = "text/plain";
            }
            String charset = AsyncHttpProviderUtils.parseCharset(contentType);
            if (charset == null) {
                charset = "utf-8";
                List<String> contentTypeList = new ArrayList<String>();
                contentTypeList.add(contentType + "; charset=utf-8");
                headers.replace(HttpHeaders.Names.CONTENT_TYPE, contentTypeList);
            }

            byte[] bodyBytes;
            try {
                bodyBytes = stringBody.getBytes(charset);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            // If using a POST with OAuth signing, the builder looks at
            // getFormParams() rather than getBody() and constructs the signature
            // based on the form params.
            if (contentType.equals(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
                Map<String, List<String>> stringListMap = FormUrlEncodedParser.parseAsJava(stringBody, "utf-8");
                for (String key : stringListMap.keySet()) {
                    List<String> values = stringListMap.get(key);
                    for (String value : values) {
                        builder.addFormParam(key, value);
                    }
                }
            } else {
                builder.setBody(stringBody);
            }

            builder.setHeaders(headers);
            builder.setBodyEncoding(charset);
        } else if (body instanceof JsonNode) {
            JsonNode jsonBody = (JsonNode) body;
            FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap(this.headers);
            List<String> contentType = new ArrayList<String>();
            contentType.add("application/json; charset=utf-8");
            headers.replace(HttpHeaders.Names.CONTENT_TYPE, contentType);
            String bodyStr = Json.stringify(jsonBody);
            byte[] bodyBytes;
            try {
                bodyBytes = bodyStr.getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            builder.setBody(bodyStr);
            builder.setHeaders(headers);
            builder.setBodyEncoding("utf-8");
        } else if (body instanceof File) {
            File fileBody = (File) body;
            FileBodyGenerator bodyGenerator = new FileBodyGenerator(fileBody);
            builder.setBody(bodyGenerator);
        } else if (body instanceof InputStream) {
            InputStream inputStreamBody = (InputStream) body;
            InputStreamBodyGenerator bodyGenerator = new InputStreamBodyGenerator(inputStreamBody);
            builder.setBody(bodyGenerator);
        } else {
            throw new IllegalStateException("Impossible body: " + body);
        }

        if (this.timeout > 0) {
            builder.setRequestTimeout(this.timeout);
        }

        if (this.followRedirects != null) {
            builder.setFollowRedirects(this.followRedirects);
        }
        if (this.virtualHost != null) {
            builder.setVirtualHost(this.virtualHost);
        }

        if (this.username != null && this.password != null && this.scheme != null) {
            builder.setRealm(auth(this.username, this.password, this.scheme));
        }

        if (this.calculator != null) {
            if (this.calculator instanceof OAuth.OAuthCalculator) {
                OAuthSignatureCalculator calc = ((OAuth.OAuthCalculator) this.calculator).getCalculator();
                builder.setSignatureCalculator(calc);
            } else {
                throw new IllegalStateException("Use OAuth.OAuthCalculator");
            }
        }

        return builder.build();
    }

    private F.Promise<WSResponse> execute(Request request) {

        final scala.concurrent.Promise<play.libs.ws.WSResponse> scalaPromise = scala.concurrent.Promise$.MODULE$.<play.libs.ws.WSResponse>apply();
        try {
            AsyncHttpClient asyncHttpClient = (AsyncHttpClient) client.getUnderlying();
            asyncHttpClient.executeRequest(request, new AsyncCompletionHandler<com.ning.http.client.Response>() {
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
        } catch (RuntimeException exception) {
            scalaPromise.failure(exception);
        }
        return F.Promise.wrap(scalaPromise.future());
    }

    Realm auth(String username, String password, WSAuthScheme scheme) {
        Realm.AuthScheme authScheme = Realm.AuthScheme.valueOf(scheme.name());
        return (new Realm.RealmBuilder())
                .setScheme(authScheme)
                .setPrincipal(username)
                .setPassword(password)
                .setUsePreemptiveAuth(true)
                .build();
    }


}
