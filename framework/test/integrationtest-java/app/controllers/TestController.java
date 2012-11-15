package controllers;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for arbitrary tests.
 *
 * The echo method just echos everything passed to it, and the slave method returns whatever it's told to.
 *
 * Mostly useful for testing the WS API, but also may be useful for testing some features of Play.
 */
public class TestController extends Controller {

    @BodyParser.Of(BodyParser.Raw.class)
    public static Result echo() {
        Echo echo = new Echo();
        echo.method = request().method();
        echo.headers = request().headers();
        echo.queryString = request().queryString();
        echo.version = request().version();
        echo.host = request().host();
        echo.remoteAddress = request().remoteAddress();
        echo.uri = request().uri();
        echo.path = request().path();
        echo.session = new HashMap<String, String>(session());
        echo.flash = new HashMap<String, String>(flash());
        echo.body = request().body().asRaw().asBytes();
        return ok(Json.toJson(echo));
    }

    public static class Echo {
        public String method;
        public Map<String, String[]> headers;
        public Map<String, String[]> queryString;
        public String version;
        public String host;
        public String remoteAddress;
        public String uri;
        public String path;
        public Map<String, String> session;
        public Map<String, String> flash;
        public byte[] body;
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result slave() {
        ToReturn toReturn = Json.fromJson(request().body().asJson(), ToReturn.class);
        for (Map.Entry<String, String> header : toReturn.headers.entrySet()) {
            response().setHeader(header.getKey(), header.getValue());
        }

        for (Cookie cookie: toReturn.cookies) {
            response().setCookie(cookie.name, cookie.value, cookie.maxAge, cookie.path, cookie.domain, cookie.secure, cookie.httpOnly);
        }

        for (Map.Entry<String, String> item : toReturn.session.entrySet()) {
            session(item.getKey(), item.getValue());
        }
        for (Map.Entry<String, String> item : toReturn.flash.entrySet()) {
            flash(item.getKey(), item.getValue());
        }

        if (toReturn.body != null) {
            return status(toReturn.status, toReturn.body);
        } else {
            return status(toReturn.status);
        }
    }

    public static class ToReturn {
        public int status = 200;
        public byte[] body;
        public Map<String, String> headers = new HashMap<String, String>();
        public List<Cookie> cookies = new ArrayList<Cookie>();
        public Map<String, String> session = new HashMap<String, String>();
        public Map<String, String> flash = new HashMap<String, String>();
    }

    public static class Cookie {
        public String name;
        public String value;
        public Integer maxAge;
        public String path;
        public String domain;
        public boolean secure = false;
        public boolean httpOnly = false;
    }
}
