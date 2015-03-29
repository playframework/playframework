package play.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

import play.api.http.MediaRange;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CallTest {

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

}

final class TestCall extends Call {
    private final String u;
    private final String m;

    TestCall(String u, String m) {
        this.u = u;
        this.m = m;
    }

    public String url() { return this.u; }
    public String method() { return this.m; }
}
