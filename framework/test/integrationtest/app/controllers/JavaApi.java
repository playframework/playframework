package controllers;

import java.util.*;

import play.*;
import play.mvc.*;
import play.mvc.Http.Cookie;

import static play.libs.Json.toJson;

public class JavaApi extends Controller {

    public static Result headers() {
       return ok(request().getHeader(HOST));
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
}

