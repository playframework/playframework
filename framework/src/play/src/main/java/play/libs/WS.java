package play.libs;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilderBase;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Realm.RealmBuilder;
import com.ning.http.client.FluentStringsMap;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import play.libs.Scala;
import play.libs.F.Promise;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Asynchronous API to to query web services, as an http client.
 *
 * The value returned is a Promise<Response>, and you should use Play's asynchronous mechanisms to use this response.
 */
public class WS {

    private static AsyncHttpClient client = play.api.libs.ws.WS.client();

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

        public WSRequest(String method) {
            super(WSRequest.class, method, false);
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

        private Promise<Response> execute() {
            final play.api.libs.concurrent.STMPromise<Response> scalaPromise = new play.api.libs.concurrent.STMPromise<Response>();
            try {
                WS.client.executeRequest(request, new AsyncCompletionHandler<com.ning.http.client.Response>() {
                    @Override
                    public com.ning.http.client.Response onCompleted(com.ning.http.client.Response response) {
                        final com.ning.http.client.Response ahcResponse = response;
                        scalaPromise.redeem(new scala.runtime.AbstractFunction0<Response>() {
                            public Response apply() {
                                return new Response(ahcResponse);
                            }
                        });
                        return response;
                    }
                    @Override
                    public void onThrowable(Throwable t) {
                        scalaPromise.throwing(t);
                    }
                });
            } catch (IOException exception) {
                scalaPromise.throwing(exception);
            }
            return new Promise(scalaPromise);
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
                Collection<String> values = headers.get(name);
                values.add(value);
            } else {
                List<String> values = new ArrayList<String>();
                values.add(value);
                queryParameters.put(name, values);
            }
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
         * Perform a OPTION on the request asynchronously.
         */
        public Promise<Response> option() {
            return execute("OPTION");
        }

        private Promise<Response> execute(String method) {
            WSRequest req = new WSRequest(method).setUrl(url)
                                                 .setHeaders(headers)
                                                 .setQueryParameters(new FluentStringsMap(queryParameters));
            if (this.username != null && this.password != null && this.scheme != null)
                req.auth(this.username, this.password, this.scheme);
            return req.execute();
        }

        private Promise<Response> executeString(String method, String body) {
            WSRequest req = new WSRequest(method).setBody(body)
                                                 .setUrl(url)
                                                 .setHeaders(headers)
                                                 .setQueryParameters(new FluentStringsMap(queryParameters));
            if (this.username != null && this.password != null && this.scheme != null)
                req.auth(this.username, this.password, this.scheme);
            return req.execute();
        }

        private Promise<Response> executeIS(String method, InputStream body) {
            WSRequest req = new WSRequest(method).setBody(body)
                                                 .setUrl(url)
                                                 .setHeaders(headers)
                                                 .setQueryParameters(new FluentStringsMap(queryParameters));
            if (this.username != null && this.password != null && this.scheme != null)
                req.auth(this.username, this.password, this.scheme);
            return req.execute();
        }

        private Promise<Response> executeFile(String method, File body) {
            WSRequest req = new WSRequest(method).setBody(body)
                                                 .setUrl(url)
                                                 .setHeaders(headers)
                                                 .setQueryParameters(new FluentStringsMap(queryParameters));
            if (this.username != null && this.password != null && this.scheme != null)
                req.auth(this.username, this.password, this.scheme);
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
            } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}



