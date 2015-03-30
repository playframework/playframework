/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.ws;

import javaguide.testhelpers.MockJavaAction;

// #ws-imports
import play.libs.ws.*;
import play.libs.F.Function;
import play.libs.F.Promise;
// #ws-imports

// #json-imports
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
// #json-imports

import java.io.*;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Document;
import play.mvc.Result;

import javax.inject.Inject;

// #ws-custom-client-imports
import com.ning.http.client.*;
import play.api.libs.ws.WSClientConfig;
import play.api.libs.ws.ning.NingWSClientConfig;
import play.api.libs.ws.ning.NingWSClientConfigFactory;
import play.api.libs.ws.ssl.SSLConfigFactory;
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder;
import scala.concurrent.duration.Duration;
// #ws-custom-client-imports

public class JavaWS {
    private static final String feedUrl = "http://localhost:3333/feed";

    public static class Controller0 extends MockJavaAction {

        private WSClient ws;

        public void requestExamples() {
            // #ws-holder
            WSRequest request = ws.url("http://example.com");
            // #ws-holder

            // #ws-complex-holder
            WSRequest complexRequest = request.setHeader("headerKey", "headerValue")
                                                    .setRequestTimeout(1000)
                                                    .setQueryParameter("paramKey", "paramValue");
            // #ws-complex-holder

            // #ws-get
            Promise<WSResponse> responsePromise = complexRequest.get();
            // #ws-get

            String url = "http://example.com";
            // #ws-auth
            ws.url(url).setAuth("user", "password", WSAuthScheme.BASIC).get();
            // #ws-auth

            // #ws-follow-redirects
            ws.url(url).setFollowRedirects(true).get();
            // #ws-follow-redirects

            // #ws-query-parameter
            ws.url(url).setQueryParameter("paramKey", "paramValue");
            // #ws-query-parameter

            // #ws-header
            ws.url(url).setHeader("headerKey", "headerValue").get();
            // #ws-header

            String jsonString = "{\"key1\":\"value1\"}";
            // #ws-header-content-type
            ws.url(url).setHeader("Content-Type", "application/json").post(jsonString);
            // OR
            ws.url(url).setContentType("application/json").post(jsonString);
            // #ws-header-content-type

            // #ws-timeout
            ws.url(url).setRequestTimeout(1000).get();
            // #ws-timeout

            // #ws-post-form-data
            ws.url(url).setContentType("application/x-www-form-urlencoded")
                       .post("key1=value1&key2=value2");
            // #ws-post-form-data

            // #ws-post-json
            JsonNode json = Json.newObject()
                                .put("key1", "value1")
                                .put("key2", "value2");

            ws.url(url).post(json);
            // #ws-post-json
        }

        public void responseExamples() {

          String url = "http://example.com";

            // #ws-response-json
            Promise<JsonNode> jsonPromise = ws.url(url).get().map(response -> {
                return response.asJson();
            });
            // #ws-response-json

            // #ws-response-xml
            Promise<Document> documentPromise = ws.url(url).get().map(response -> {
                return response.asXml();
            });
            // #ws-response-xml

            // #ws-response-input-stream
            Promise<File> filePromise = ws.url(url).get().map(response -> {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = response.getBodyAsStream();

                    // write the inputStream to a File
                    final File file = new File("/tmp/response.txt");
                    outputStream = new FileOutputStream(file);

                    int read = 0;
                    byte[] buffer = new byte[1024];

                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }

                    return file;
                } catch (IOException e) {
                    throw e;
                } finally {
                    if (inputStream != null) {inputStream.close();}
                    if (outputStream != null) {outputStream.close();}
                }
            });
            // #ws-response-input-stream
        }

        public void patternExamples() {
            String urlOne = "http://localhost:3333/one";
            // #ws-composition
            final Promise<WSResponse> responseThreePromise = ws.url(urlOne).get()
                    .flatMap(responseOne -> ws.url(responseOne.getBody()).get())
                    .flatMap(responseTwo -> ws.url(responseTwo.getBody()).get());
            // #ws-composition

            // #ws-recover
            Promise<WSResponse> responsePromise = ws.url("http://example.com").get();
            Promise<WSResponse> recoverPromise = responsePromise.recoverWith(throwable ->
                            ws.url("http://backup.example.com").get()
            );
            // #ws-recover
        }

        public void clientExamples() {
            // #ws-client
            WSClient client = WS.client();
            // #ws-client

            // #ws-custom-client
            // Set up the client config (you can also use a parser here):
            scala.Option<String> noneString = scala.None$.empty();
            WSClientConfig wsClientConfig = new WSClientConfig(
                    Duration.apply(120, TimeUnit.SECONDS), // connectionTimeout
                    Duration.apply(120, TimeUnit.SECONDS), // idleTimeout
                    Duration.apply(120, TimeUnit.SECONDS), // requestTimeout
                    true, // followRedirects
                    true, // useProxyProperties
                    noneString, // userAgent
                    true, // compressionEnabled / enforced
                    SSLConfigFactory.defaultConfig());

            NingWSClientConfig clientConfig = NingWSClientConfigFactory.forClientConfig(wsClientConfig);

            // Build a secure config out of the client config:
            NingAsyncHttpClientConfigBuilder secureBuilder = new NingAsyncHttpClientConfigBuilder(clientConfig);
            AsyncHttpClientConfig secureDefaults = secureBuilder.build();

            // You can directly use the builder for specific options once you have secure TLS defaults...
            AsyncHttpClientConfig customConfig = new AsyncHttpClientConfig.Builder(secureDefaults)
                            .setProxyServer(new com.ning.http.client.ProxyServer("127.0.0.1", 38080))
                            .setCompressionEnforced(true)
                            .build();
            WSClient customClient = new play.libs.ws.ning.NingWSClient(customConfig);

            Promise<WSResponse> responsePromise = customClient.url("http://example.com/feed").get();
            // #ws-custom-client

            // #ws-underlying-client
            com.ning.http.client.AsyncHttpClient underlyingClient =
                (com.ning.http.client.AsyncHttpClient) ws.getUnderlying();
            // #ws-underlying-client

        }
    }

    public static class Controller1 extends MockJavaAction {

        @Inject
        private WSClient ws;

        // #ws-action
        public Promise<Result> index() {
            return ws.url(feedUrl).get().map(response ->
                            ok("Feed title: " + response.asJson().findPath("title").asText())
            );
        }
        // #ws-action
    }

    public static class Controller2 extends MockJavaAction {

        @Inject
        private WSClient ws;

        // #composed-call
        public Promise<Result> index() {
            return ws.url(feedUrl).get()
                    .flatMap(response -> ws.url(response.asJson().findPath("commentsUrl").asText()).get())
                    .map(response -> ok("Number of comments: " + response.asJson().findPath("count").asInt()));
        }
        // #composed-call
    }

}
