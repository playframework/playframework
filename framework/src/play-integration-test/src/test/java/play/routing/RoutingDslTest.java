/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.routing;

import org.junit.Test;
import play.api.routing.Router;
import play.libs.F;
import play.mvc.PathBindable;
import play.mvc.Result;
import play.mvc.Results;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

/**
 * This class is in the integration tests so that we have the right helper classes to build a request with to test it.
 */
public class RoutingDslTest {

    @Test
    public void noParameters() {
        Router router = new RoutingDsl()
                .GET("/hello/world").routeTo(() -> Results.ok("Hello world"))
                .build();

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void oneParameter() {
        Router router = new RoutingDsl()
                .GET("/hello/:to").routeTo(new F.Function<String, Result>() {
                    public Result apply(String to) {
                        return Results.ok("Hello " + to);
                    }
                })
                .build();

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void twoParameters() {
        Router router = new RoutingDsl()
                .GET("/:say/:to").routeTo((say, to) -> Results.ok(say + " " + to))
                .build();

        assertThat(makeRequest(router, "GET", "/Hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo"));
    }

    @Test
    public void threeParameters() {
        Router router = new RoutingDsl()
                .GET("/:say/:to/:extra").routeTo((say, to, extra) -> Results.ok(say + " " + to + extra))
                .build();

        assertThat(makeRequest(router, "GET", "/Hello/world/!"), equalTo("Hello world!"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void noParametersAsync() {
        Router router = new RoutingDsl()
                .GET("/hello/world").routeAsync(() -> F.Promise.pure(Results.ok("Hello world")))
                .build();

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void oneParameterAsync() {
        Router router = new RoutingDsl()
                .GET("/hello/:to").routeAsync(to -> F.Promise.pure(Results.ok("Hello " + to)))
                .build();

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void twoParametersAsync() {
        Router router = new RoutingDsl()
                .GET("/:say/:to").routeAsync((say, to) -> F.Promise.pure(Results.ok(say + " " + to)))
                .build();

        assertThat(makeRequest(router, "GET", "/Hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/foo"));
    }

    @Test
    public void threeParametersAsync() {
        Router router = new RoutingDsl()
                .GET("/:say/:to/:extra").routeAsync((say, to, extra) -> F.Promise.pure(Results.ok(say + " " + to + extra)))
                .build();

        assertThat(makeRequest(router, "GET", "/Hello/world/!"), equalTo("Hello world!"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void get() {
        Router router = new RoutingDsl()
                .GET("/hello/world").routeTo(() -> Results.ok("Hello world"))
                .build();

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void head() {
        Router router = new RoutingDsl()
                .HEAD("/hello/world").routeTo(() -> Results.ok("Hello world"))
                .build();

        assertThat(makeRequest(router, "HEAD", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void post() {
        Router router = new RoutingDsl()
                .POST("/hello/world").routeTo(() -> Results.ok("Hello world"))
                .build();

        assertThat(makeRequest(router, "POST", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/hello/world"));
    }

    @Test
    public void put() {
        Router router = new RoutingDsl()
                .PUT("/hello/world").routeTo(() -> Results.ok("Hello world"))
                .build();

        assertThat(makeRequest(router, "PUT", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void delete() {
        Router router = new RoutingDsl()
                .DELETE("/hello/world").routeTo(() -> Results.ok("Hello world"))
                .build();

        assertThat(makeRequest(router, "DELETE", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void patch() {
        Router router = new RoutingDsl()
                .PATCH("/hello/world").routeTo(() -> Results.ok("Hello world"))
                .build();

        assertThat(makeRequest(router, "PATCH", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void options() {
        Router router = new RoutingDsl()
                .OPTIONS("/hello/world").routeTo(() -> Results.ok("Hello world"))
                .build();

        assertThat(makeRequest(router, "OPTIONS", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "POST", "/hello/world"));
    }

    @Test
    public void starMatcher() {
        Router router = new RoutingDsl()
                .GET("/hello/*to").routeTo((to) -> Results.ok("Hello " + to))
                .build();

        assertThat(makeRequest(router, "GET", "/hello/blah/world"), equalTo("Hello blah/world"));
        assertNull(makeRequest(router, "GET", "/foo/bar"));
    }

    @Test
    public void regexMatcher() {
        Router router = new RoutingDsl()
                .GET("/hello/$to<[a-z]+>").routeTo((to) -> Results.ok("Hello " + to))
                .build();

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertNull(makeRequest(router, "GET", "/hello/10"));
    }
    
    @Test
    public void multipleRoutes() {
        Router router = new RoutingDsl()
                .GET("/hello/:to").routeTo((to) -> Results.ok("Hello " + to))
                .GET("/foo/bar").routeTo(() -> Results.ok("foo bar"))
                .POST("/hello/:to").routeTo((to) -> Results.ok("Post " + to))
                .GET("/*path").routeTo((path) -> Results.ok("Path " + path))
                .build();

        assertThat(makeRequest(router, "GET", "/hello/world"), equalTo("Hello world"));
        assertThat(makeRequest(router, "GET", "/foo/bar"), equalTo("foo bar"));
        assertThat(makeRequest(router, "POST", "/hello/world"), equalTo("Post world"));
        assertThat(makeRequest(router, "GET", "/something/else"), equalTo("Path something/else"));
    }

    @Test
    public void encoding() {
        Router router = new RoutingDsl()
                .GET("/simple/:to").routeTo((to) -> Results.ok("Simple " + to))
                .GET("/path/*to").routeTo((to) -> Results.ok("Path " + to))
                .GET("/regex/$to<.*>").routeTo((to) -> Results.ok("Regex " + to))
                .build();

        assertThat(makeRequest(router, "GET", "/simple/dollar%24"), equalTo("Simple dollar$"));
        assertThat(makeRequest(router, "GET", "/path/dollar%24"), equalTo("Path dollar%24"));
        assertThat(makeRequest(router, "GET", "/regex/dollar%24"), equalTo("Regex dollar%24"));
    }

    @Test
    public void typed() {
        Router router = new RoutingDsl()
                .GET("/:a/:b/:c").routeTo((Integer a, Boolean b, String c) ->
                        Results.ok("int " + a + " boolean " + b + " string " + c))
                .build();

        assertThat(makeRequest(router, "GET", "/20/true/foo"), equalTo("int 20 boolean true string foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongNumberOfParameters() {
        new RoutingDsl().GET("/:a/:b").routeTo(foo -> Results.ok(foo.toString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badParameterType() {
        new RoutingDsl().GET("/:a").routeTo((InputStream is) -> Results.ok());
    }

    @Test
    public void bindError() {
        Router router = new RoutingDsl()
                .GET("/:a").routeTo((Integer a) -> Results.ok("int " + a))
                .build();

        assertThat(makeRequest(router, "GET", "/foo"),
                equalTo("Cannot parse parameter a as Int: For input string: \"foo\""));
    }

    @Test
    public void customPathBindable() {
        Router router = new RoutingDsl()
                .GET("/:a").routeTo((MyString myString) -> Results.ok(myString.value))
                .build();

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
