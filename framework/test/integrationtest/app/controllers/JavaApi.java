package controllers;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;

import play.*;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Cookie;

import static play.libs.Json.toJson;
import static play.libs.Jsonp.jsonp;

public class JavaApi extends Controller {

    public static Result headers() {
       return ok(request().getHeader(HOST));
    }
    public static Result cookietest() {
       response().setCookie("testcookie", "blue");
       return ok(request().cookies().get("testcookie").name());
    }

    public static Result index() {
        Map<String, String> d = new HashMap<String, String>();
        d.put("peter", "foo");
        d.put("yay", "value");
        return ok(toJson(d));
    }
    
    public static Result setCookie() {
        response().setCookie("foo", "bar");
        return ok();
    }
    
    public static Result readCookie(String name) {
        Cookie cookie = request().cookies().get(name);
        if (cookie != null) {
            return ok("Cookie " + name + " has value: " + cookie.value());
        } else {
            return ok();
        }
    }
    
    public static Result clearCookie(String name) {
        response().discardCookies(name);
        return ok();
    }
    
    public static Result notIntercepted() {
        return ok(Interceptor.state);
    }
    
    @With(Interceptor.class)
    public static Result interceptedUsingWith() {
        return ok(Interceptor.state);
    }
    
    @Intercepted
    public static Result intercepted() {
        return ok(Interceptor.state);
    }

    public static Result takeList(List<Integer> xs) {
        return ok(xs.size() + " elements");
    }

    public static Result jsonpJava(String callback) {
        JsonNode json = Json.parse("{ \"foo\": \"bar\" }");
        return ok(jsonp(callback, json));
    }

    public static Result accept() {
        if (request().accepts("application/json")) {
            return ok("json");
        } else if (request().accepts("text/html")) {
            return ok("html");
        } else {
            return badRequest();
        }
    }

    public static Promise<Result> promised() {
        return Promise.<Result>pure(ok("x"));
    }

}
