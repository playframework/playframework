package play.libs.ws.ahc;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.*;
import play.libs.ws.StandaloneWSRequest;
import play.libs.ws.StandaloneWSResponse;
import play.mvc.Http;
import play.mvc.MultipartFormatter;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * A Play WS request backed by AsyncHTTPClient implementation.
 */
public class AhcWSRequest implements WSRequest {

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
    public WSRequest setMultipartBody(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        String boundary = MultipartFormatter.randomBoundary();
        this.request.setBody(MultipartFormatter.transform(body, boundary));
        setHeader("Content-Type", MultipartFormatter.boundaryToContentType(boundary));
        return this;
    }

    @Override
    public CompletionStage<WSResponse> get() {
        return request.get().thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(String body) {
        return request.patch(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(JsonNode body) {
        return request.patch(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(InputStream body) {
        return request.patch(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> patch(File body) {
        return request.patch(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(String body) {
        return request.post(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(JsonNode body) {
        return request.post(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(InputStream body) {
        return request.post(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> post(File body) {
        return request.post(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(String body) {
        return request.put(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(JsonNode body) {
        return request.put(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(InputStream body) {
        return request.put(body).thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> put(File body) {
        return request.put(body).thenApply(responseFunction);
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
        return request.execute().thenApply(responseFunction);
    }

    @Override
    public CompletionStage<WSResponse> execute() {
        return request.execute().thenApply(responseFunction);
    }

    @Override
    public CompletionStage<StreamedResponse> stream() {
        return request.stream();
    }

    @Override
    public WSRequest setMethod(String method) {
        return converter.apply(request.setMethod(method));
    }

    @Override
    public WSRequest setBody(String body) {
        return converter.apply(request.setBody(body));
    }

    @Override
    public WSRequest setBody(JsonNode body) {
        return converter.apply(request.setBody(body));
    }

    @Deprecated
    @Override
    public WSRequest setBody(InputStream body) {
        return converter.apply(request.setBody(body));
    }

    @Override
    public WSRequest setBody(File body) {
        return converter.apply(request.setBody(body));
    }

    @Override
    public <U> WSRequest setBody(Source<ByteString, U> body) {
        return converter.apply(request.setBody(body));
    }

    @Override
    public WSRequest setHeader(String name, String value) {
        return converter.apply(request.setHeader(name, value));
    }

    @Override
    public WSRequest setQueryString(String query) {
        return converter.apply(request.setQueryString(query));
    }

    @Override
    public WSRequest setQueryParameter(String name, String value) {
        return converter.apply(request.setQueryParameter(name, value));
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

    @Override
    public WSRequest setRequestTimeout(long timeout) {
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
    public String getUrl() {
        return request.getUrl();
    }

    @Override
    public Map<String, Collection<String>> getHeaders() {
        return request.getHeaders();
    }

    @Override
    public Map<String, Collection<String>> getQueryParameters() {
        return request.getQueryParameters();
    }

    @Override
    public String getUsername() {
        return request.getUsername();
    }

    @Override
    public String getPassword() {
        return request.getPassword();
    }

    @Override
    public WSAuthScheme getScheme() {
        return request.getScheme();
    }

    @Override
    public WSSignatureCalculator getCalculator() {
        return request.getCalculator();
    }

    @Override
    public long getRequestTimeout() {
        return request.getRequestTimeout();
    }

    @Override
    public boolean getFollowRedirects() {
        return request.getFollowRedirects();
    }

    @Override
    public CompletionStage<WSResponse> patch(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        setMethod("PATCH");
        setMultipartBody(body);
        return execute();
    }

    @Override
    public CompletionStage<WSResponse> post(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        setMethod("POST");
        setMultipartBody(body);
        return execute();
    }

    @Override
    public CompletionStage<WSResponse> put(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        setMethod("PUT");
        setMultipartBody(body);
        return execute();
    }

}
