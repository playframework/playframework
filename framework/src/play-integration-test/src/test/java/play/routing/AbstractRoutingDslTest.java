/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing;

import org.junit.Test;
import play.mvc.PathBindable;
import play.mvc.Result;
import play.mvc.Results;
import play.test.WithApplication;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

/**
 * This class is in the integration tests so that we have the right helper classes to build a request with to test it.
 */
public abstract class AbstractRoutingDslTest extends WithApplication {

    abstract RoutingDsl routingDsl();

    abstract Router router(RoutingDsl routing);

    @Test
    public void noParameters() {
        RoutingDsl routing = routingDsl()
                .GET("/hello/world").routeTo(() -> Results.ok("Hello world"));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void oneParameter() {
        RoutingDsl routing = routingDsl()
                .GET("/hello/:to").routeTo(to -> Results.ok("Hello " + to));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void twoParameters() {
        RoutingDsl routing = routingDsl()
                .GET("/:say/:to").routeTo((say, to) -> Results.ok(say + " " + to));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/Hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo"));
    }

    @Test
    public void threeParameters() {
        RoutingDsl routing = routingDsl()
                .GET("/:say/:to/:extra").routeTo((say, to, extra) -> Results.ok(say + " " + to + extra));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/Hello/world/!"), equalTo("Hello world!"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void noParametersAsync() {
        RoutingDsl routing = routingDsl()
                .GET("/hello/world").routeAsync(() -> CompletableFuture.completedFuture(Results.ok("Hello world")));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void oneParameterAsync() {
        RoutingDsl routing = routingDsl()
                .GET("/hello/:to").routeAsync(to -> CompletableFuture.completedFuture(Results.ok("Hello " + to)));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void twoParametersAsync() {
        RoutingDsl routing = routingDsl()
                .GET("/:say/:to").routeAsync((say, to) -> CompletableFuture.completedFuture(Results.ok(say + " " + to)));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/Hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo"));
    }

    @Test
    public void threeParametersAsync() {
        RoutingDsl routing = routingDsl()
                .GET("/:say/:to/:extra").routeAsync((say, to, extra) -> CompletableFuture.completedFuture(
                Results.ok(say + " " + to + extra)));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/Hello/world/!"), equalTo("Hello world!"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void get() {
        RoutingDsl routing = routingDsl()
                .GET("/hello/world").routeTo(() -> Results.ok("Hello world"));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void head() {
        RoutingDsl routing = routingDsl()
                .HEAD("/hello/world").routeTo(() -> Results.ok("Hello world"));

        Router router = router(routing);

        assertThat(makeRequest(router, "HEAD", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void post() {
        RoutingDsl routing = routingDsl()
                .POST("/hello/world").routeTo(() -> Results.ok("Hello world"));

        Router router = router(routing);

        assertThat(makeRequest(router, "POST", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/hello/world"));
    }

    @Test
    public void put() {
        RoutingDsl routing = routingDsl()
                .PUT("/hello/world").routeTo(() -> Results.ok("Hello world"));

        Router router = router(routing);

        assertThat(makeRequest(router, "PUT", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void delete() {
        RoutingDsl routing = routingDsl()
                .DELETE("/hello/world").routeTo(() -> Results.ok("Hello world"));

        Router router = router(routing);

        assertThat(makeRequest(router, "DELETE", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void patch() {
        RoutingDsl routing = routingDsl()
                .PATCH("/hello/world").routeTo(() -> Results.ok("Hello world"));

        Router router = router(routing);

        assertThat(makeRequest(router, "PATCH", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void options() {
        RoutingDsl routing = routingDsl()
                .OPTIONS("/hello/world").routeTo(() -> Results.ok("Hello world"));

        Router router = router(routing);

        assertThat(makeRequest(router, "OPTIONS", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void starMatcher() {
        RoutingDsl routing = routingDsl()
                .GET("/hello/*to").routeTo((to) -> Results.ok("Hello " + to));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/hello/blah/world"), equalTo("Hello blah/world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void regexMatcher() {
        RoutingDsl routing = routingDsl()
                .GET("/hello/$to<[a-z]+>").routeTo((to) -> Results.ok("Hello " + to));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/hello/10"));
    }

    @Test
    public void multipleRoutes() {
        RoutingDsl routing = routingDsl()
                .GET("/hello/:to").routeTo((to) -> Results.ok("Hello " + to))
                .GET("/foo/bar").routeTo(() -> Results.ok("foo bar"))
                .POST("/hello/:to").routeTo((to) -> Results.ok("Post " + to))
                .GET("/*path").routeTo((path) -> Results.ok("Path " + path));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertThat(makeRequest(router, "GET", "/foo/bar"), equalTo("foo bar"));
        assertThat(makeRequest(router, "POST", "/hello/world"), equalTo("Post world"));
        assertThat(makeRequest(router, "GET", "/something/else"), equalTo("Path something/else"));
    }

    @Test
    public void encoding() {
        RoutingDsl routing = routingDsl()
                .GET("/simple/:to").routeTo((to) -> Results.ok("Simple " + to))
                .GET("/path/*to").routeTo((to) -> Results.ok("Path " + to))
                .GET("/regex/$to<.*>").routeTo((to) -> Results.ok("Regex " + to));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/simple/dollar%24"), equalTo("Simple dollar$"));
        assertThat(makeRequest(router, "GET", "/path/dollar%24"), equalTo("Path dollar%24"));
        assertThat(makeRequest(router, "GET", "/regex/dollar%24"), equalTo("Regex dollar%24"));
    }

    @Test
    public void typed() {
        RoutingDsl routing = routingDsl()
                .GET("/:a/:b/:c").routeTo((Integer a, Boolean b, String c) ->
                        Results.ok("int " + a + " boolean " + b + " string " + c));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/20/true/foo"), equalTo("int 20 boolean true string foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongNumberOfParameters() {
        routingDsl().GET("/:a/:b").routeTo(foo -> Results.ok(foo.toString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badParameterType() {
        routingDsl().GET("/:a").routeTo((InputStream is) -> Results.ok());
    }

    @Test
    public void bindError() {
        RoutingDsl routing = routingDsl()
                .GET("/:a").routeTo((Integer a) -> Results.ok("int " + a));

        Router router = router(routing);

        assertThat(makeRequest(router, "GET", "/foo"),
                equalTo("Cannot parse parameter a as Int: For input string: \"foo\""));
    }

    @Test
    public void customPathBindable() {
        RoutingDsl routing = routingDsl()
                .GET("/:a").routeTo((MyString myString) -> Results.ok(myString.value));

        Router router = router(routing);

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
        Result result = routeAndCall(router, fakeRequest(method, path));
        if (result == null) {
            return null;
        } else {
            return contentAsString(result);
        }
    }

}
