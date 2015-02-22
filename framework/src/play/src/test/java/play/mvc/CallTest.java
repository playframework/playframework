package play.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
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
                     "http://playframework.com/url",
                     call.absoluteURL(req));
    }

    @Test
    public void testHttpAbsoluteURL2() throws Throwable {
        final TestRequest req =
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

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
        final TestRequest req =
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("Absolute URL should have HTTPS scheme",
                     "https://playframework.com/url",
                     call.absoluteURL(req));
    }

    @Test
    public void testHttpsAbsoluteURL2() throws Throwable {
        final TestRequest req =
            new TestRequest("GET", "playframework.com", "/playframework",
                            false, "127.0.0.1", "/v", "/u");

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
        final TestRequest req =
            new TestRequest("GET", "playframework.com", "/playframework",
                            false, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTP scheme",
                     "ws://playframework.com/url",
                     call.webSocketURL(req));
    }

    @Test
    public void testWebSocketURL2() throws Throwable {
        final TestRequest req =
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

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
        final TestRequest req =
            new TestRequest("GET", "playframework.com", "/playframework",
                            true, "127.0.0.1", "/v", "/u");

        final TestCall call = new TestCall("/url", "GET");

        assertEquals("WebSocket URL should have HTTPS scheme",
                     "wss://playframework.com/url",
                     call.webSocketURL(req));
    }

    @Test
    public void testSecureWebSocketURL2() throws Throwable {
        final TestRequest req =
            new TestRequest("GET", "playframework.com", "/playframework",
                            false, "127.0.0.1", "/v", "/u");

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
    private final TreeMap<String,String[]> hs =
        new TreeMap<String,String[]>(play.core.utils.CaseInsensitiveOrdered$.MODULE$);

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
