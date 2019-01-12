/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CallTest {

    @Test
    public void testHttpURL1() throws Throwable {
        final TestCall call = new TestCall("/myurl", "GET");

        assertEquals("Call should return correct url in path()",
                     "/myurl",
                     call.path());
    }

    @Test
    public void testHttpURL2() throws Throwable {
        final Call call = new TestCall("/myurl", "GET").withFragment("myfragment");

        assertEquals("Call should return correct url and fragment in path()",
                     "/myurl#myfragment",
                     call.path());
    }

    @Test
    public void testHttpAbsoluteURL1() throws Throwable {
        final Request req = new RequestBuilder()
            .uri("http://playframework.com/playframework").build();

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTP scheme",
                     "http://playframework.com/url",
                     call.absoluteURL(req));
    }

    @Test
    public void testHttpAbsoluteURL2() throws Throwable {
        final Request req = new RequestBuilder()
            .uri("https://playframework.com/playframework").build();

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTP scheme",
                     "http://playframework.com/url",
                     call.absoluteURL(req, false));
    }

    @Test
    public void testHttpAbsoluteURL3() throws Throwable {
        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTP scheme",
                     "http://typesafe.com/url",
                     call.absoluteURL(false, "typesafe.com"));
    }

    @Test
    public void testHttpsAbsoluteURL1() throws Throwable {
        final Request req = new RequestBuilder()
            .uri("https://playframework.com/playframework").build();

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTPS scheme",
                     "https://playframework.com/url",
                     call.absoluteURL(req));
    }

    @Test
    public void testHttpsAbsoluteURL2() throws Throwable {
        final Request req = new RequestBuilder()
            .uri("http://playframework.com/playframework").build();

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTPS scheme",
                     "https://playframework.com/url",
                     call.absoluteURL(req, true));
    }

    @Test
    public void testHttpsAbsoluteURL3() throws Throwable {
        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTPS scheme",
                     "https://typesafe.com/url",
                     call.absoluteURL(true, "typesafe.com"));
    }

    @Test
    public void testWebSocketURL1() throws Throwable {
        final Request req = new RequestBuilder()
            .uri("http://playframework.com/playframework").build();

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTP scheme",
                     "ws://playframework.com/url",
                     call.webSocketURL(req));
    }

    @Test
    public void testWebSocketURL2() throws Throwable {
        final Request req = new RequestBuilder()
            .uri("https://playframework.com/playframework").build();

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTP scheme",
                     "ws://playframework.com/url",
                     call.webSocketURL(req, false));
    }

    @Test
    public void testWebSocketURL3() throws Throwable {
        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTP scheme",
                     "ws://typesafe.com/url",
                     call.webSocketURL(false, "typesafe.com"));
    }

    @Test
    public void testSecureWebSocketURL1() throws Throwable {
        final Request req = new RequestBuilder()
            .uri("https://playframework.com/playframework").build();

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTPS scheme",
                     "wss://playframework.com/url",
                     call.webSocketURL(req));
    }

    @Test
    public void testSecureWebSocketURL2() throws Throwable {
        final Request req = new RequestBuilder()
            .uri("http://playframework.com/playframework").build();

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTPS scheme",
                     "wss://playframework.com/url",
                     call.webSocketURL(req, true));
    }

    @Test
    public void testSecureWebSocketURL3() throws Throwable {
        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTPS scheme",
                     "wss://typesafe.com/url",
                     call.webSocketURL(true, "typesafe.com"));
    }

    @Test
    public void testRelative1() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two").build();

        final TestCall call = new TestCall("/one/two-b", "GET");

        assertEquals("Relative path takes start path from Request",
                "two-b",
                call.relativeTo(req));
    }

    @Test
    public void testRelative2() throws Throwable {
        final String startPath = "/one/two";

        final TestCall call = new TestCall("/one/two-b", "GET");

        assertEquals("Relative path takes start path as String",
                "two-b",
                call.relativeTo(startPath));
    }

    @Test
    public void testRelative3() throws Throwable {
        final Request req = new RequestBuilder()
                .uri("http://playframework.com/one/two").build();

        final TestCall call = new TestCall("/one/two-b", "GET", "foo");

        assertEquals("Relative path includes fragment",
                "two-b#foo",
                call.relativeTo(req));
    }

    @Test
    public void testCanonical() throws Throwable {
        final TestCall call = new TestCall("/one/.././two//three-b", "GET");

        assertEquals("Canonical path returned from Call",
                "/two/three-b",
                call.canonical());
    }

}

final class TestCall extends Call {
    private final String u;
    private final String m;
    private final String f;

    TestCall(String u, String m) {
        this.u = u;
        this.m = m;
        this.f = null;
    }

    TestCall(String u, String m, String f) {
        this.u = u;
        this.m = m;
        this.f = f;
    }

    public String url() { return this.u; }
    public String method() { return this.m; }
    public String fragment() { return this.f; }
}
