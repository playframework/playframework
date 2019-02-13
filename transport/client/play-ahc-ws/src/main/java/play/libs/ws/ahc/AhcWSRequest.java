/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;
import play.libs.ws.*;
import play.mvc.Http;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * A Play WS request backed by AsyncHTTPClient implementation.
 */
public class AhcWSRequest implements WSRequest {
    private static WSBodyWritables writables = new WSBodyWritables() {};

    private final AhcWSClient client;
    private final StandaloneAhcWSRequest request;
    private final Function<StandaloneWSResponse, WSResponse> responseFunction = AhcWSResponse::new;
    private final Function<StandaloneWSRequest, WSRequest> converter = new Function<StandaloneWSRequest, WSRequest>() {
        public WSRequest apply(StandaloneWSRequest standaloneWSRequest) {
            final StandaloneAhcWSRequest plainAhcWSRequest = (StandaloneAhcWSRequest) standaloneWSRequest;
            return new AhcWSRequest(client, plainAhcWSRequest);
        }
    };

    AhcWSRequest(AhcWSClient client, StandaloneAhcWSRequest request) {
        this.client = client;
        this.request = request;
    }

    @Override
    public CompletionStage<WSResponse> get() {
        return request.get().thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(BodyWritable body) {
        return request.patch(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(String string) {
        return request.patch(writables.body(string)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(JsonNode jsonNode) {
        return request.patch(writables.body(jsonNode)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(Document doc) {
        return request.patch(writables.body(doc)).thenApply(responseFunction);
    }

    @Deprecated
    @Override
    public CompletionStage<WSResponse> patch(InputStream inputStream) {
        return request.patch(writables.body(() -> inputStream)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(File file) {
        return request.patch(writables.body(file)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> bodyPartSource) {
        return request.patch(writables.multipartBody(bodyPartSource)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(BodyWritable body) {
        return request.post(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(String string) {
        return request.post(writables.body(string)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(JsonNode json) {
        return request.post(writables.body(json)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(Document doc) {
        return request.post(writables.body(doc)).thenApply(responseFunction);
    }

    @Override
    @Deprecated
    public CompletionStage<WSResponse> post(InputStream is) {
        return request.post(writables.body(() -> is)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(File file) {
        return request.post(writables.body(file)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> bodyPartSource) {
        return request.post(writables.multipartBody(bodyPartSource)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(BodyWritable body) {
        return request.put(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(String string) {
        return request.put(writables.body(string)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(JsonNode json) {
        return request.put(writables.body(json)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(Document doc) {
        return request.put(writables.body(doc)).thenApply(responseFunction);
    }

    @Override
    @Deprecated
    public CompletionStage<WSResponse> put(InputStream is) {
        return request.put(writables.body(() -> is)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(File file) {
        return request.put(writables.body(file)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> bodyPartSource) {
        return request.put(writables.multipartBody(bodyPartSource)).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> delete() {
        return request.delete().thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> head() {
        return request.head().thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> options() {
        return request.options().thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> execute(String method) {
        return request.setMethod(method).execute().thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> execute() {
        return request.execute().thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> stream() {
        return request.stream().thenApply(responseFunction);
    }

    @Override
    public WSRequest setMethod(String method) {
        return converter.apply(request.setMethod(method));
    }

    @Override
    public WSRequest setBody(BodyWritable bodyWritable) {
        return converter.apply(request.setBody(bodyWritable));
    }

    @Override
    public WSRequest setBody(String string) {
        return converter.apply(request.setBody(writables.body(string)));
    }

    @Override
    public WSRequest setBody(JsonNode json) {
        return converter.apply(request.setBody(writables.body(json)));
    }

    @Deprecated
    @Override
    public WSRequest setBody(InputStream is) {
        return converter.apply(request.setBody(writables.body(() -> is)));
    }

    @Override
    public WSRequest setBody(File file) {
        return converter.apply(request.setBody(writables.body(file)));
    }

    @Override
    public <U> WSRequest setBody(Source<ByteString, U> source) {
        return converter.apply(request.setBody(writables.body(source)));
    }

    /**
     * @deprecated use addHeader(name, value)
     */
    @Deprecated
    @Override
    public WSRequest setHeader(String name, String value) {
        return converter.apply(request.addHeader(name, value));
    }

    @Override
    public WSRequest setHeaders(Map<String, List<String>> headers) {
        return converter.apply(request.setHeaders(headers));
    }

    @Override
    public WSRequest addHeader(String name, String value) {
        return converter.apply(request.addHeader(name, value));
    }

    @Override
    public WSRequest setQueryString(String query) {
        return converter.apply(request.setQueryString(query));
    }

    /**
     * @deprecated Use addQueryParameter
     */
    @Deprecated
    @Override
    public WSRequest setQueryParameter(String name, String value) {
        return converter.apply(request.addQueryParameter(name, value));
    }

    @Override
    public WSRequest addQueryParameter(String name, String value) {
        return converter.apply(request.addQueryParameter(name, value));
    }

    @Override
    public WSRequest setQueryString(Map<String, List<String>> params) {
        return converter.apply(request.setQueryString(params));
    }

    @Override
    public StandaloneWSRequest setUrl(String url) {
        return converter.apply(request.setUrl(url));
    }

    @Override
    public WSRequest addCookie(WSCookie cookie) {
        return converter.apply(request.addCookie(cookie));
    }

    @Override
    public WSRequest addCookie(Http.Cookie cookie) {
        return converter.apply(request.addCookie(asCookie(cookie)));
    }

    private WSCookie asCookie(Http.Cookie cookie) {
        return new DefaultWSCookie(cookie.name(), cookie.value(),
                cookie.domain(),
                cookie.path(),
                Optional.ofNullable(cookie.maxAge()).map(Integer::longValue).filter(f -> f > -1L).orElse(null),
                cookie.secure(),
                cookie.httpOnly());
    }

    @Override
    public WSRequest addCookies(WSCookie... cookies) {
        return converter.apply(request.addCookies(cookies));
    }

    @Override
    public WSRequest setCookies(List<WSCookie> cookies) {
        return converter.apply(request.setCookies(cookies));
    }

    @Override
    public WSRequest setAuth(String userInfo) {
        return converter.apply(request.setAuth(userInfo));
    }

    @Override
    public WSRequest setAuth(String username, String password) {
        return converter.apply(request.setAuth(username, password));
    }

    @Override
    public WSRequest setAuth(String username, String password, WSAuthScheme scheme) {
        return converter.apply(request.setAuth(username, password, scheme));
    }

    @Override
    public StandaloneWSRequest setAuth(WSAuthInfo authInfo) {
        return converter.apply(request.setAuth(authInfo));
    }

    @Override
    public WSRequest sign(WSSignatureCalculator calculator) {
        return converter.apply(request.sign(calculator));
    }

    @Override
    public WSRequest setFollowRedirects(boolean followRedirects) {
        return converter.apply(request.setFollowRedirects(followRedirects));
    }

    @Override
    public WSRequest setVirtualHost(String virtualHost) {
        return converter.apply(request.setVirtualHost(virtualHost));
    }

    /**
     * @deprecated Use {@link #setRequestTimeout(Duration timeout)}
     * @param timeout the request timeout in milliseconds. A value of -1 indicates an infinite request timeout.
     */
    @Deprecated
    @Override
    public WSRequest setRequestTimeout(long timeout) {
        Duration d;
        if (timeout == -1) {
            d = Duration.of(1, ChronoUnit.YEARS);
        } else {
            d = Duration.ofMillis(timeout);
        }
        return converter.apply(request.setRequestTimeout(d));
    }

    @Override
    public WSRequest setRequestTimeout(Duration timeout) {
        return converter.apply(request.setRequestTimeout(timeout));
    }

    @Override
    public WSRequest setRequestFilter(WSRequestFilter filter) {
        return converter.apply(request.setRequestFilter(filter));
    }

    @Override
    public WSRequest setContentType(String contentType) {
        return converter.apply(request.setContentType(contentType));
    }

    @Override
    public Optional<WSAuthInfo> getAuth() {
        return request.getAuth();
    }

    @Override
    public Optional<BodyWritable> getBody() {
        return request.getBody();
    }

    @Override
    public Optional<WSSignatureCalculator> getCalculator() {
        return request.getCalculator();
    }

    @Override
    public Optional<String> getContentType() {
        return request.getContentType();
    }

    @Override
    public Optional<Boolean> getFollowRedirects() {
        return request.getFollowRedirects();
    }

    @Override
    public String getUrl() {
        return request.getUrl();
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return request.getHeaders();
    }

    @Override
    public List<String> getHeaderValues(String name) {
        return request.getHeaderValues(name);
    }

    @Override
    public Optional<String> getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public Optional<Duration> getRequestTimeout() {
        return request.getRequestTimeout();
    }

    @Override
    public Map<String, List<String>> getQueryParameters() {
        return request.getQueryParameters();
    }

}
