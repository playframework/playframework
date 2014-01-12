package play.libs;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.PerRequestConfig;
import com.ning.http.client.RequestBuilderBase;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Realm.RealmBuilder;
import com.ning.http.client.FluentStringsMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.ning.http.util.AsyncHttpProviderUtils;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.w3c.dom.Document;

import play.libs.F.Promise;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Asynchronous API to to query web services, as an http client.
 *
 * The value returned is a {@code Promise<Response>}, and you should use Play's asynchronous mechanisms to use this response.
 */
public class WS {

    private static AsyncHttpClient client() {
        return play.api.libs.ws.WS.client();
    }

    /**
     * Prepare a new request. You can then construct it by chaining calls.
     *
     * @param url the URL to request
     */
    public static WSRequestHolder url(String url) {
        return new WSRequestHolder(url);
    }

    /**
     * Provides the bridge between Play and the underlying ning request
     */
    public static class WSRequest extends RequestBuilderBase<WSRequest> {

        private FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();

        private String method;

        private String url;

        public WSRequest(String method) {
            super(WSRequest.class, method, false);
            this.method = method;
        }

        private WSRequest auth(String username, String password, AuthScheme scheme) {
            this.setRealm((new RealmBuilder())
                    .setScheme(scheme)
                    .setPrincipal(username)
                    .setPassword(password)
                    .setUsePreemptiveAuth(true)
                    .build());
            return this;
        }

        /**
         * Set an HTTP header.
         */
        @Override
        public WSRequest setHeader(String name, String value) {
            headers.replace(name, value);
            return super.setHeader(name, value);
        }

        /**
         * Add an HTTP header (used for headers with mutiple values).
         */
        @Override
        public WSRequest addHeader(String name, String value) {
            if (value == null) {
                value = "";
            }
            headers.add(name, value);
            return super.addHeader(name, value);
        }

        /**
         * Defines the request headers.
         */
        @Override
        public WSRequest setHeaders(FluentCaseInsensitiveStringsMap hdrs) {
            headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : headers);
            return super.setHeaders(hdrs);
        }

        /**
         * Defines the request headers.
         */
        @Override
        public WSRequest setHeaders(Map<String, Collection<String>> hdrs) {
            headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : new FluentCaseInsensitiveStringsMap(headers));
            return super.setHeaders(hdrs);
        }

        /**
         * Return the headers of the request being constructed
         */
        public Map<String, List<String>> getAllHeaders() {
            return headers;
        }

        public List<String> getHeader(String name) {
            List<String> hdrs = headers.get(name);
            if (hdrs == null) return new ArrayList<String>();
            return hdrs;
        }

        public String getMethod() {
            return this.method;
        }

        @Override
        public WSRequest setUrl(String url) {
            this.url = url;
            return super.setUrl(url);
        }

        public String getUrl() {
            return this.url;
        }

        public Promise<Response> execute() {
            final scala.concurrent.Promise<Response> scalaPromise = scala.concurrent.Promise$.MODULE$.<Response>apply();
            try {
                WS.client().executeRequest(request, new AsyncCompletionHandler<com.ning.http.client.Response>() {
                    @Override
                    public com.ning.http.client.Response onCompleted(com.ning.http.client.Response response) {
                        final com.ning.http.client.Response ahcResponse = response;
                        scalaPromise.success(new Response(ahcResponse));
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
            return new Promise<Response>(scalaPromise.future());
        }
    }

    /**
     * provides the User facing API for building WS request.
     */
    public static class WSRequestHolder {

        private final String url;
        private Map<String, Collection<String>> headers = new HashMap<String, Collection<String>>();
        private Map<String, Collection<String>> queryParameters = new HashMap<String, Collection<String>>();

        private String username = null;
        private String password = null;
        private AuthScheme scheme = null;
        private SignatureCalculator calculator = null;

        private int timeout = 0;
        private Boolean followRedirects = null;

        public WSRequestHolder(String url) {
            this.url = url;
        }

        /**
         * Sets a header with the given name, this can be called repeatedly.
         *
         * @param name
         * @param value
         */
        public WSRequestHolder setHeader(String name, String value) {
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
         * Sets a query parameter with the given name,this can be called repeatedly.
         *
         * @param name
         * @param value
         */
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
         * @param username
         * @param password
         */
        public WSRequestHolder setAuth(String username, String password) {
            this.username = username;
            this.password = password;
            this.scheme = AuthScheme.BASIC;
            return this;
        }

        /**
         * Sets the authentication header for the current request.
         *
         * @param username
         * @param password
         * @param scheme authentication scheme
         */
        public WSRequestHolder setAuth(String username, String password, AuthScheme scheme) {
            this.username = username;
            this.password = password;
            this.scheme = scheme;
            return this;
        }

        public WSRequestHolder sign(SignatureCalculator calculator) {
            this.calculator = calculator;
            return this;
        }

        /**
         * Sets whether redirects (301, 302) should be followed automatically.
         *
         * @param followRedirects
         */
        public WSRequestHolder setFollowRedirects(Boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        /**
         * Sets the request timeout in milliseconds.
         *
         * @param timeout
         */
        public WSRequestHolder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Set the content type.  If the request body is a String, and no charset parameter is included, then it will
         * default to UTF-8.
         *
         * @param contentType The content type
         */
        public WSRequestHolder setContentType(String contentType) {
            return setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType);
        }

        /**
         * @return the URL of the request.
         */
        public String getUrl() {
            return this.url;
        }

        /**
         * @return the headers (a copy to prevent side-effects).
         */
        public Map<String, Collection<String>> getHeaders() {
            return new HashMap<String, Collection<String>>(this.headers);
        }

        /**
         * @return the query parameters (a copy to prevent side-effects).
         */
        public Map<String, Collection<String>> getQueryParameters() {
            return new HashMap<String, Collection<String>>(this.queryParameters);
        }

        /**
         * @return the auth username, null if not an authenticated request.
         */
        public String getUsername() {
            return this.username;
        }

        /**
         * @return the auth password, null if not an authenticated request
         */
        public String getPassword() {
            return this.password;
        }

        /**
         * @return the auth scheme, null if not an authenticated request
         */
        public AuthScheme getScheme() {
            return this.scheme;
        }

        /**
         * @return the signature calculator (exemple: OAuth), null if none is set.
         */
        public SignatureCalculator getCalculator() {
            return this.calculator;
        }

        /**
         * @return the auth scheme (null if not an authenticated request)
         */
        public int getTimeout() {
            return this.timeout;
        }

        /**
         * @return true if the request is configure to follow redirect, false if it is configure not to, null if nothing is configured and the global client preference should be used instead.
         */
        public Boolean getFollowRedirects() {
            return this.followRedirects;
        }

        /**
         * Perform a GET on the request asynchronously.
         */
        public Promise<Response> get() {
            return execute("GET");
        }

        /**
         * Perform a POST on the request asynchronously.
         *
         * @param body represented as String
         */
        public Promise<Response> post(String body) {
            return executeString("POST", body);
        }

        /**
         * Perform a PUT on the request asynchronously.
         *
         * @param body represented as String
         */
        public Promise<Response> put(String body) {
            return executeString("PUT", body);
        }

        /**
         * Perform a POST on the request asynchronously.
         *
         * @param body represented as JSON
         */
        public Promise<Response> post(JsonNode body) {
            return executeJson("POST", body);
        }

        /**
         * Perform a PUT on the request asynchronously.
         *
         * @param body represented as JSON
         */
        public Promise<Response> put(JsonNode body) {
            return executeJson("PUT", body);
        }

        /**
         * Perform a POST on the request asynchronously.
         *
         * @param body represented as an InputStream
         */
        public Promise<Response> post(InputStream body) {
            return executeIS("POST", body);
        }

        /**
         * Perform a PUT on the request asynchronously.
         *
         * @param body represented as an InputStream
         */
        public Promise<Response> put(InputStream body) {
            return executeIS("PUT", body);
        }

        /**
         * Perform a POST on the request asynchronously.
         *
         * @param body represented as a File
         */
        public Promise<Response> post(File body) {
            return executeFile("POST", body);
        }

        /**
         * Perform a PUT on the request asynchronously.
         *
         * @param body represented as a File
         */
        public Promise<Response> put(File body) {
            return executeFile("PUT", body);
        }

        /**
         * Perform a DELETE on the request asynchronously.
         */
        public Promise<Response> delete() {
            return execute("DELETE");
        }

        /**
         * Perform a HEAD on the request asynchronously.
         */
        public Promise<Response> head() {
            return execute("HEAD");
        }

        /**
         * Perform an OPTIONS on the request asynchronously.
         */
        public Promise<Response> options() {
            return execute("OPTIONS");
        }

        /**
         * Execute an arbitrary method on the request asynchronously.
         *
         * @param method The method to execute
         */
        public Promise<Response> execute(String method) {
            WSRequest req = new WSRequest(method).setUrl(url)
                    .setHeaders(headers)
                    .setQueryParameters(new FluentStringsMap(queryParameters));
            return execute(req);
        }

        private Promise<Response> executeString(String method, String body) {
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

            WSRequest req = new WSRequest(method).setBody(body)
                    .setUrl(url)
                    .setHeaders(headers)
                    .setQueryParameters(new FluentStringsMap(queryParameters))
                    .setBodyEncoding(charset);
            return execute(req);
        }

        private Promise<Response> executeJson(String method, JsonNode body) {
            WSRequest req = new WSRequest(method).setBody(Json.stringify(body))
                    .setUrl(url)
                    .setHeaders(headers)
                    .setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8")
                    .setQueryParameters(new FluentStringsMap(queryParameters))
                    .setBodyEncoding("utf-8");
            return execute(req);

        }

        private Promise<Response> executeIS(String method, InputStream body) {
            WSRequest req = new WSRequest(method).setBody(body)
                    .setUrl(url)
                    .setHeaders(headers)
                    .setQueryParameters(new FluentStringsMap(queryParameters));
            return execute(req);
        }

        private Promise<Response> executeFile(String method, File body) {
            WSRequest req = new WSRequest(method).setBody(body)
                    .setUrl(url)
                    .setHeaders(headers)
                    .setQueryParameters(new FluentStringsMap(queryParameters));
            return execute(req);
        }

        private Promise<Response> execute(WSRequest req) {
            if (this.timeout > 0) {
                PerRequestConfig config = new PerRequestConfig();
                config.setRequestTimeoutInMs(this.timeout);
                req.setPerRequestConfig(config);
            }
            if (this.followRedirects != null) {
                req.setFollowRedirects(this.followRedirects);
            }
            if (this.username != null && this.password != null && this.scheme != null)
                req.auth(this.username, this.password, this.scheme);
            if (this.calculator != null)
                this.calculator.sign(req);
            return req.execute();
        }
    }

    /**
     * A WS Cookie.
     */
    public static interface Cookie {

        /**
         * Returns the underlying "native" object for the cookie.
         */
        public Object getUnderlying();

        public String getDomain();

        public String getName();

        public String getValue();

        public String getPath();

        public Integer getMaxAge();

        public Boolean isSecure();

        public Integer getVersion();

        // Cookie ports should not be used; cookies for a given host are shared across
        // all the ports on that host.
    }

    /**
     * The Ning implementation of a WS cookie.
     */
    private static class NingCookie implements Cookie {

        private final com.ning.http.client.Cookie ahcCookie;

        public NingCookie(com.ning.http.client.Cookie ahcCookie) {
            this.ahcCookie = ahcCookie;
        }

        /**
         * Returns the underlying "native" object for the cookie.
         */
        public Object getUnderlying() {
            return ahcCookie;
        }

        public String getDomain() {
            return ahcCookie.getDomain();
        }

        public String getName() {
            return ahcCookie.getName();
        }

        public String getValue() {
            return ahcCookie.getValue();
        }

        public String getPath() {
            return ahcCookie.getPath();
        }

        public Integer getMaxAge() {
            return ahcCookie.getMaxAge();
        }

        public Boolean isSecure() {
            return ahcCookie.isSecure();
        }

        public Integer getVersion() {
            return ahcCookie.getVersion();
        }
    }

    /**
     * A WS response.
     */
    public static class Response {

        private com.ning.http.client.Response ahcResponse;

        public Response(com.ning.http.client.Response ahcResponse) {
            this.ahcResponse = ahcResponse;
        }

        /**
         * Get the HTTP status code of the response
         */
        public int getStatus() {
            return ahcResponse.getStatusCode();
        }

        /**
         * Get the HTTP status text of the response
         */
        public String getStatusText() {
            return ahcResponse.getStatusText();
        }

        /**
         * Get the given HTTP header of the response
         */
        public String getHeader(String key) {
            return ahcResponse.getHeader(key);
        }

        /**
         * Get all the cookies.
         */
        public List<Cookie> getCookies() {
            List<Cookie> cookieList = new ArrayList<Cookie>();
            for (com.ning.http.client.Cookie ahcCookie : ahcResponse.getCookies()) {
                cookieList.add(new NingCookie(ahcCookie));
            }
            return cookieList;
        }

        /**
         * Get only one cookie, using the cookie name.
         */
        public Cookie getCookie(String name) {
            for (com.ning.http.client.Cookie ahcCookie : ahcResponse.getCookies()) {
                // safe -- cookie.getName() will never return null
                if (ahcCookie.getName().equals(name)) {
                    return new NingCookie(ahcCookie);
                }
            }
            return null;
        }

        /**
         * Get the response body as a string.  If the charset is not specified, this defaults to ISO-8859-1 for text
         * sub mime types, as per RFC-2616 sec 3.7.1, otherwise it defaults to UTF-8.
         */
        public String getBody() {
            try {
                // RFC-2616#3.7.1 states that any text/* mime type should default to ISO-8859-1 charset if not
                // explicitly set, while Plays default encoding is UTF-8.  So, use UTF-8 if charset is not explicitly
                // set and content type is not text/*, otherwise default to ISO-8859-1
                String contentType = ahcResponse.getContentType();
                if (contentType == null) {
                    // As defined by RFC-2616#7.2.1
                    contentType = "application/octet-stream";
                }
                String charset = AsyncHttpProviderUtils.parseCharset(contentType);

                if (charset != null) {
                    return ahcResponse.getResponseBody(charset);
                } else if (contentType.startsWith("text/")) {
                    return ahcResponse.getResponseBody(AsyncHttpProviderUtils.DEFAULT_CHARSET);
                } else {
                    return ahcResponse.getResponseBody("utf-8");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Get the response body as a {@link Document DOM document}
         * @return a DOM document
         */
        public Document asXml() {
            try {
                return play.libs.XML.fromInputStream(ahcResponse.getResponseBodyAsStream(), "utf-8");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Get the response body as a {@link com.fasterxml.jackson.databind.JsonNode}
         * @return the json response
         */
        public JsonNode asJson() {
            try {
                // Jackson will automatically detect the correct encoding according to the rules in RFC-4627
                return Json.parse(ahcResponse.getResponseBodyAsStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Get the response body as a stream
         * @return The stream to read the response body from
         */
        public InputStream getBodyAsStream() {
            try {
                return ahcResponse.getResponseBodyAsStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Get the response body as a byte array
         * @return The byte array
         */
        public byte[] asByteArray() {
            try {
                return ahcResponse.getResponseBodyAsBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Return the request {@link java.net.URI}. Note that if the request got redirected, the value of the
         * {@link java.net.URI} will be the last valid redirect url.
         *
         * @return the request {@link java.net.URI}.
         */
        public URI getUri() {
            try {
                return ahcResponse.getUri();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Sign a WS call.
     */
    public static interface SignatureCalculator {

        /**
         * Sign a request
         */
        public void sign(WSRequest request);

    }

}



