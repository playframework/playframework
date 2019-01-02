/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import akka.util.ByteString;
import org.junit.Test;
import play.Application;
import play.libs.Json;
import play.libs.XML;
import play.mvc.Http;
import play.mvc.PathBindable;
import play.mvc.Result;
import play.mvc.Results;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;
import static play.mvc.Results.ok;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * This class is in the integration tests so that we have the right helper classes to build a request with to test it.
 */
public abstract class AbstractRoutingDslTest {

    abstract Application application();

    abstract RoutingDsl routingDsl();

    private Router router(Function<RoutingDsl, Router> function) {
        return function.apply(routingDsl());
    }

    @Test
    public void shouldProvideJavaRequestToActionWithoutParameters() {
        Router router = router(routingDsl -> routingDsl.GET("/with-request")
                .routingTo(request ->
                        request.header("X-Test")
                            .map(Results::ok)
                            .orElse(Results.notFound())).build());

        String result = makeRequest(
                router,
                "GET",
                "/with-request",
                rb -> rb.header("X-Test", "Header value")
        );
        assertThat(result, equalTo("Header value"));
    }

    @Test
    public void shouldProvideJavaRequestToActionWithSingleParameter() {
        Router router = router(routingDsl -> routingDsl.GET("/with-request/:p1")
                .routingTo((request, number) ->
                        request.header("X-Test")
                            .map(header -> Results.ok(header + " - " + number))
                            .orElse(Results.notFound())).build());

        String result = makeRequest(
                router,
                "GET",
                "/with-request/10",
                rb -> rb.header("X-Test", "Header value")
        );
        assertThat(result, equalTo("Header value - 10"));
    }

    @Test
    public void shouldProvideJavaRequestToActionWith2Parameters() {
        Router router = router(routingDsl -> routingDsl.GET("/with-request/:p1/:p2")
                .routingTo((request, n1, n2) ->
                        request.header("X-Test")
                            .map(header -> Results.ok(header + " - " + n1 + " - " + n2))
                            .orElse(Results.notFound())).build());

        String result = makeRequest(
                router,
                "GET",
                "/with-request/10/20",
                rb -> rb.header("X-Test", "Header value")
        );
        assertThat(result, equalTo("Header value - 10 - 20"));
    }

    @Test
    public void shouldProvideJavaRequestToActionWith3Parameters() {
        Router router = router(routingDsl -> routingDsl.GET("/with-request/:p1/:p2/:p3")
                .routingTo((request, n1, n2, n3) ->
                        request.header("X-Test")
                                .map(header -> Results.ok(header + " - " + n1 + " - " + n2 + " - " + n3))
                                .orElse(Results.notFound())).build());

        String result = makeRequest(
                router,
                "GET",
                "/with-request/10/20/30",
                rb -> rb.header("X-Test", "Header value")
        );
        assertThat(result, equalTo("Header value - 10 - 20 - 30"));
    }

    @Test
    public void shouldProvideJavaRequestToAsyncActionWithoutParameters() {
        Router router = router(routingDsl -> routingDsl.GET("/with-request")
                .routingAsync(request ->
                    CompletableFuture.completedFuture(
                        request.header("X-Test")
                            .map(Results::ok)
                            .orElse(Results.notFound())
                    )
                ).build());

        String result = makeRequest(
                router,
                "GET",
                "/with-request",
                rb -> rb.header("X-Test", "Header value")
        );
        assertThat(result, equalTo("Header value"));
    }

    @Test
    public void shouldProvideJavaRequestToAsyncActionWithSingleParameter() {
        Router router = router(routingDsl -> routingDsl.GET("/with-request/:p1")
                .routingAsync((request, number) ->
                    CompletableFuture.completedFuture(
                        request.header("X-Test")
                            .map(header -> Results.ok(header + " - " + number))
                            .orElse(Results.notFound())
                    )
                ).build());

        String result = makeRequest(
                router,
                "GET",
                "/with-request/10",
                rb -> rb.header("X-Test", "Header value")
        );
        assertThat(result, equalTo("Header value - 10"));
    }

    @Test
    public void shouldProvideJavaRequestToAsyncActionWith2Parameters() {
        Router router = router(routingDsl -> routingDsl.GET("/with-request/:p1/:p2")
                .routingAsync((request, n1, n2) ->
                    CompletableFuture.completedFuture(
                        request.header("X-Test")
                            .map(header -> Results.ok(header + " - " + n1 + " - " + n2))
                            .orElse(Results.notFound())
                    )
                ).build());

        String result = makeRequest(
                router,
                "GET",
                "/with-request/10/20",
                rb -> rb.header("X-Test", "Header value")
        );
        assertThat(result, equalTo("Header value - 10 - 20"));
    }

    @Test
    public void shouldProvideJavaRequestToAsyncActionWith3Parameters() {
        Router router = router(routingDsl -> routingDsl.GET("/with-request/:p1/:p2/:p3")
                .routingAsync((request, n1, n2, n3) ->
                    CompletableFuture.completedFuture(
                        request.header("X-Test")
                            .map(header -> Results.ok(header + " - " + n1 + " - " + n2 + " - " + n3))
                            .orElse(Results.notFound())
                        )
                ).build());

        String result = makeRequest(
                router,
                "GET",
                "/with-request/10/20/30",
                rb -> rb.header("X-Test", "Header value")
        );
        assertThat(result, equalTo("Header value - 10 - 20 - 30"));
    }

    @Test
    public void shouldPreserveRequestBodyAsText() {
        Router router = router(routingDsl -> routingDsl.POST("/with-body")
                .routingTo(request -> Results.ok(request.body().asText()))
                .build());

        String result = makeRequest(
                router,
                "POST",
                "/with-body",
                rb -> rb.bodyText("The Body")
        );
        assertThat(result, equalTo("The Body"));
    }

    @Test
    public void shouldPreserveRequestBodyAsJson() {
        Router router = router(routingDsl -> routingDsl.POST("/with-body")
                .routingTo(request -> Results.ok(request.body().asJson()))
                .build());

        String result = makeRequest(
                router,
                "POST",
                "/with-body",
                requestBuilder -> requestBuilder.bodyJson(Json.parse("{ \"a\": \"b\" }"))
        );
        assertThat(result, equalTo("{\"a\":\"b\"}"));
    }

    @Test
    public void shouldPreserveRequestBodyAsXml() {
        Router router = router(routingDsl -> routingDsl.POST("/with-body")
                .routingTo(request -> ok(XML.toBytes(request.body().asXml()).utf8String()))
                .build());

        String result = makeRequest(
                router,
                "POST",
                "/with-body",
                requestBuilder -> requestBuilder.bodyXml(XML.fromString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>b</a>"))
        );
        assertThat(result, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>b</a>"));
    }

    @Test
    public void shouldPreserveRequestBodyAsRawBuffer() {
        Router router = router(routingDsl -> routingDsl.POST("/with-body")
                .routingTo(request -> ok(request.body().asRaw().asBytes().utf8String()))
                .build());

        String result = makeRequest(
                router,
                "POST",
                "/with-body",
                requestBuilder -> requestBuilder.bodyRaw(ByteString.fromString("The Raw Body"))
        );
        assertThat(result, equalTo("The Raw Body"));
    }

    @Test
    public void shouldPreserveRequestBodyAsTextWhenUsingHttpContext() {
        Router router = router(routingDsl -> routingDsl.POST("/with-body")
            .routeTo(() -> {
                // This better emulates how users will access the request object
                Http.Request request = Http.Context.current().request();
                return ok(request.body().asText());
            }).build());

        String result = makeRequest(
                router,
                "POST",
                "/with-body",
                rb -> rb.bodyText("The Body")
        );
        assertThat(result, equalTo("The Body"));
    }

    @Test
    public void shouldPreserveRequestBodyAsJsonWhenUsingHttpContext() {
        Router router = router(routingDsl -> routingDsl.POST("/with-body")
                .routeTo(() -> {
                    // This better emulates how users will access the request object
                    Http.Request request = Http.Context.current().request();
                    return ok(request.body().asJson());
                }).build());

        String result = makeRequest(
                router,
                "POST",
                "/with-body",
                requestBuilder -> requestBuilder.bodyJson(Json.parse("{ \"a\": \"b\" }"))
        );
        assertThat(result, equalTo("{\"a\":\"b\"}"));
    }

    @Test
    public void shouldPreserveRequestBodyAsXmlWhenUsingHttpContext() {
        Router router = router(routingDsl -> routingDsl.POST("/with-body")
                .routeTo(() -> {
                    // This better emulates how users will access the request object
                    Http.Request request = Http.Context.current().request();
                    return ok(XML.toBytes(request.body().asXml()).utf8String());
                }).build());

        String result = makeRequest(
                router,
                "POST",
                "/with-body",
                requestBuilder -> requestBuilder.bodyXml(XML.fromString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>b</a>"))
        );
        assertThat(result, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><a>b</a>"));
    }

    @Test
    public void shouldPreserveRequestBodyAsRawBufferWhenUsingHttpContext() {
        Router router = router(routingDsl -> routingDsl.POST("/with-body")
                .routeTo(() -> {
                    // This better emulates how users will access the request object
                    Http.Request request = Http.Context.current().request();
                    return ok(request.body().asRaw().asBytes().utf8String());
                }).build());

        String result = makeRequest(
                router,
                "POST",
                "/with-body",
                requestBuilder -> requestBuilder.bodyRaw(ByteString.fromString("The Raw Body"))
        );
        assertThat(result, equalTo("The Raw Body"));
    }

    @Test
    public void noParameters() {
        Router router = router(routingDsl ->
            routingDsl.GET("/hello/world").routeTo(() -> ok("Hello world")).build()
        );

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void oneParameter() {
        Router router = router(routingDsl ->
            routingDsl.GET("/hello/:to").routeTo(to -> ok("Hello " + to)).build()
        );

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void twoParameters() {
        Router router = router(routingDsl ->
            routingDsl.GET("/:say/:to").routeTo((say, to) -> ok(say + " " + to)).build()
        );

        assertThat(makeRequest(router, "GET", "/Hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo"));
    }

    @Test
    public void threeParameters() {
        Router router = router(routingDsl ->
            routingDsl.GET("/:say/:to/:extra").routeTo((say, to, extra) -> ok(say + " " + to + extra)).build()
        );

        assertThat(makeRequest(router, "GET", "/Hello/world/!"), equalTo("Hello world!"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void noParametersAsync() {
        Router router = router(routingDsl ->
            routingDsl.GET("/hello/world").routeAsync(() -> completedFuture(ok("Hello world"))).build()
        );

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void oneParameterAsync() {
        Router router = router(routingDsl ->
            routingDsl.GET("/hello/:to").routeAsync(to -> completedFuture(ok("Hello " + to))).build()
        );

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void twoParametersAsync() {
        Router router = router(routingDsl ->
            routingDsl.GET("/:say/:to").routeAsync((say, to) -> completedFuture(ok(say + " " + to))).build()
        );

        assertThat(makeRequest(router, "GET", "/Hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo"));
    }

    @Test
    public void threeParametersAsync() {
        Router router = router(routingDsl ->
            routingDsl
                .GET("/:say/:to/:extra")
                .routeAsync((say, to, extra) -> completedFuture(ok(say + " " + to + extra)))
                .build()
        );

        assertThat(makeRequest(router, "GET", "/Hello/world/!"), equalTo("Hello world!"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void get() {
        Router router = router(routingDsl ->
            routingDsl.GET("/hello/world").routeTo(() -> ok("Hello world")).build()
        );

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void head() {
        Router router = router(routingDsl ->
            routingDsl.HEAD("/hello/world").routeTo(() -> ok("Hello world")).build()
        );

        assertThat(makeRequest(router, "HEAD", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void post() {
        Router router = router(routingDsl ->
            routingDsl.POST("/hello/world").routeTo(() -> ok("Hello world")).build()
        );

        assertThat(makeRequest(router, "POST", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/hello/world"));
    }

    @Test
    public void put() {
        Router router = router(routingDsl ->
            routingDsl.PUT("/hello/world").routeTo(() -> ok("Hello world")).build()
        );

        assertThat(makeRequest(router, "PUT", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void delete() {
        Router router = router(routingDsl ->
            routingDsl.DELETE("/hello/world").routeTo(() -> ok("Hello world")).build()
        );

        assertThat(makeRequest(router, "DELETE", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void patch() {
        Router router = router(routingDsl ->
            routingDsl.PATCH("/hello/world").routeTo(() -> ok("Hello world")).build()
        );

        assertThat(makeRequest(router, "PATCH", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void options() {
        Router router = router(routingDsl ->
            routingDsl.OPTIONS("/hello/world").routeTo(() -> ok("Hello world")).build()
        );

        assertThat(makeRequest(router, "OPTIONS", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void withContext() {
        Router router = router(routingDsl ->
            routingDsl.GET("/hello/world").routeTo(() -> {
                Http.Context.current().session().put("foo", "bar");
                Http.Context.current().response().setHeader("Foo", "Bar");
                return ok("Hello world");
            }).build()
        );

        Result result = routeAndCall(application(), router, fakeRequest("GET", "/hello/world"));
        assertThat(result.session().get("foo"), equalTo("bar"));
        assertThat(result.headers().get("Foo"), equalTo("Bar"));
    }

    @Test
    public void starMatcher() {
        Router router = router(routingDsl ->
            routingDsl.GET("/hello/*to").routeTo((to) -> ok("Hello " + to)).build()
        );

        assertThat(makeRequest(router, "GET", "/hello/blah/world"), equalTo("Hello blah/world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void regexMatcher() {
        Router router = router(routingDsl ->
            routingDsl.GET("/hello/$to<[a-z]+>").routeTo((to) -> ok("Hello " + to)).build()
        );

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/hello/10"));
    }

    @Test
    public void multipleRoutes() {
        Router router = router(routingDsl ->
            routingDsl
                .GET("/hello/:to").routeTo((to) -> ok("Hello " + to))
                .GET("/foo/bar").routeTo(() -> ok("foo bar"))
                .POST("/hello/:to").routeTo((to) -> ok("Post " + to))
                .GET("/*path").routeTo((path) -> ok("Path " + path))
                .build()
        );

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertThat(makeRequest(router, "GET", "/foo/bar"), equalTo("foo bar"));
        assertThat(makeRequest(router, "POST", "/hello/world"), equalTo("Post world"));
        assertThat(makeRequest(router, "GET", "/something/else"), equalTo("Path something/else"));
    }

    @Test
    public void encoding() {
        Router router = router(routingDsl ->
            routingDsl
                .GET("/simple/:to").routeTo((to) -> ok("Simple " + to))
                .GET("/path/*to").routeTo((to) -> ok("Path " + to))
                .GET("/regex/$to<.*>").routeTo((to) -> ok("Regex " + to))
                .build()
        );

        assertThat(makeRequest(router, "GET", "/simple/dollar%24"), equalTo("Simple dollar$"));
        assertThat(makeRequest(router, "GET", "/path/dollar%24"), equalTo("Path dollar%24"));
        assertThat(makeRequest(router, "GET", "/regex/dollar%24"), equalTo("Regex dollar%24"));
    }

    @Test
    public void typed() {
        Router router = router(routingDsl ->
            routingDsl
                .GET("/:a/:b/:c").routeTo((Integer a, Boolean b, String c) ->
                    ok("int " + a + " boolean " + b + " string " + c)
                ).build()
        );

        assertThat(makeRequest(router, "GET", "/20/true/foo"), equalTo("int 20 boolean true string foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongNumberOfParameters() {
        routingDsl().GET("/:a/:b").routeTo(foo -> ok(foo.toString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badParameterType() {
        routingDsl().GET("/:a").routeTo((InputStream is) -> ok());
    }

    @Test
    public void bindError() {
        Router router = router(routingDsl ->
            routingDsl.GET("/:a").routeTo((Integer a) -> ok("int " + a)).build()
        );

        assertThat(makeRequest(router, "GET", "/foo"),
                equalTo("Cannot parse parameter a as Int: For input string: \"foo\""));
    }

    @Test
    public void customPathBindable() {
        Router router = router(routingDsl ->
            routingDsl.GET("/:a").routeTo((MyString myString) -> ok(myString.value)).build()
        );

        assertThat(makeRequest(router, "GET", "/foo"), equalTo("a:foo"));
    }

    public static class MyString implements PathBindable<MyString> {
        final String value;

        public MyString() {
            this.value = null;
        }

        public MyString(String value) {
            this.value = value;
        }

        public MyString bind(String key, String txt) {
            return new MyString(key + ":" + txt);
        }

        public String unbind(String key) {
            return null;
        }

        public String javascriptUnbind() {
            return null;
        }
    }

    private String makeRequest(Router router, String method, String path) {
        return makeRequest(router, method, path, Function.identity());
    }

    private String makeRequest(Router router, String method, String path, Function<Http.RequestBuilder, Http.RequestBuilder> bodySetter) {
        Http.RequestBuilder request = bodySetter.apply(fakeRequest(method, path));
        Result result = routeAndCall(application(), router, request);
        if (result == null) {
            return null;
        } else {
            return contentAsString(result);
        }
    }

}
