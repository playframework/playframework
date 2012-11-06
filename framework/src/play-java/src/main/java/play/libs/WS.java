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

import org.w3c.dom.Document;

import play.libs.F.Promise;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Asynchronous API to to query web services, as an http client.
 *
 * The value returned is a Promise<Response>, and you should use Play's asynchronous mechanisms to use this response.
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
            headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : new FluentCaseInsensitiveStringsMap(headers));
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
         * Sets a header with the given name, this can be called repeatedly 
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
         * Sets a query parameter with the given name,this can be called repeatedly
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
         * Sets whether redirects (301, 302) should be followed automatically
         *
         * @param followRedirects
         */
        public WSRequestHolder setFollowRedirects(Boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        /**
         * Sets the request timeout in milliseconds
         *
         * @param timeout
         */
        public WSRequestHolder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
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

        private Promise<Response> execute(String method) {
            WSRequest req = new WSRequest(method).setUrl(url)
                    .setHeaders(headers)
                    .setQueryParameters(new FluentStringsMap(queryParameters));
            return execute(req);
        }

        private Promise<Response> executeString(String method, String body) {
            WSRequest req = new WSRequest(method).setBody(body)
                    .setUrl(url)
                    .setHeaders(headers)
                    .setQueryParameters(new FluentStringsMap(queryParameters));
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
         * Get the response body as a string
         */
        public String getBody() {
            try {
                return ahcResponse.getResponseBody();
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
         * Get the response body as a {@link org.codehaus.jackson.JsonNode}
         * @return the json response
         */
        public JsonNode asJson() {
            String json = getBody();
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(json, JsonNode.class);
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



