package play.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.api.http.MediaRange;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CallTest {

    @Test
    public void testHttpAbsoluteURL1() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            false, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTP scheme",
                     call.absoluteURL(req), "http://playframework.com/url");
    }

    @Test
    public void testHttpAbsoluteURL2() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTP scheme",
                     call.absoluteURL(req, false), 
                     "http://playframework.com/url");
    }

    @Test
    public void testHttpAbsoluteURL3() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTP scheme",
                     call.absoluteURL(false, "typesafe.com"), 
                     "http://typesafe.com/url");
    }

    @Test
    public void testHttpsAbsoluteURL1() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTPS scheme",
                     call.absoluteURL(req), "https://playframework.com/url");
    }

    @Test
    public void testHttpsAbsoluteURL2() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            false, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTPS scheme",
                     call.absoluteURL(req, true), 
                     "https://playframework.com/url");
    }

    @Test
    public void testHttpsAbsoluteURL3() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            false, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTPS scheme",
                     call.absoluteURL(true, "typesafe.com"), 
                     "https://typesafe.com/url");
    }

    @Test
    public void testWebSocketURL1() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            false, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTP scheme",
                     call.webSocketURL(req), "ws://playframework.com/url");
    }

    @Test
    public void testWebSocketURL2() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTP scheme",
                     call.webSocketURL(req, false), 
                     "ws://playframework.com/url");
    }

    @Test
    public void testWebSocketURL3() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTP scheme",
                     call.webSocketURL(false, "typesafe.com"), 
                     "ws://typesafe.com/url");
    }

    @Test
    public void testSecureWebSocketURL1() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTPS scheme",
                     call.webSocketURL(req), "wss://playframework.com/url");
    }

    @Test
    public void testSecureWebSocketURL2() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            false, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTPS scheme",
                     call.webSocketURL(req, true), 
                     "wss://playframework.com/url");
    }

    @Test
    public void testSecureWebSocketURL3() throws Throwable {
        final TestRequest req = 
            new TestRequest("GET", "playframework.com", "/playframework",
                            false, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTPS scheme",
                     call.webSocketURL(true, "typesafe.com"), 
                     "wss://typesafe.com/url");
    }

}

// Can't use FakeRequest from Play-Test
final class TestCookies extends ArrayList<Http.Cookie> implements Http.Cookies {
    public Http.Cookie get(String name) {
        if (name == null) return null;

        for (final Http.Cookie c : this) {
            if (c != null && name.equals(c.name())) return c;
        }

        return null; // Not found
    }
}

final class TestRequest extends Http.Request {
    private final HashMap<String,String[]> hs = 
        new HashMap<String,String[]>();

    private final HashMap<String,String[]> qs = 
        new HashMap<String,String[]>();

    private final TestCookies cs = new TestCookies();

    private final String m; // Method
    private final String h; // Host
    private final String p; // Path
    private final boolean s; // Secure
    private final String ra; // Remote address
    private final String v; // Version
    private final String u; // URI

    public TestRequest(String m, String h, String p, 
                       boolean s, String ra, String v, String u) {

        this.m = m;
        this.h = h;
        this.p = p;
        this.s = s;
        this.ra = ra;
        this.v = v;
        this.u = u;
    }

    // ---

    public String method() { return this.m; }
    public String host() { return this.h; }
    public String path() { return this.p; }
    public boolean secure() { return this.s; }
    public String remoteAddress() { return this.ra; }
    public String version() { return this.v; }
    public String uri() { return this.u; }

    public Map<String,String[]> headers() { return hs; }

    public Http.RequestBody body() { return null; /* TODO */ }

    public Http.Cookies cookies() { return this.cs; }

    public Map<String,String[]> queryString() { return this.qs; }

    public boolean accepts(String mimeType) { return true; }

    public List<MediaRange> acceptedTypes() { return null; /* TODO */ }

    public List<String> accept() { return null; /* TODO */ }

    public List<play.i18n.Lang> acceptLanguages() { return null; /* TODO */ }
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
