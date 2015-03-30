/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.*;
import play.libs.Json;
import play.test.WithApplication;
import javaguide.testhelpers.MockJavaAction;

import play.mvc.*;
import play.mvc.Http.*;

import java.util.Map;

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static play.mvc.Controller.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaResponse extends WithApplication {

    @Test
    public void textContentType() {
        //#text-content-type
        Result textResult = ok("Hello World!");
        //#text-content-type

        assertThat(textResult.header("Content-Type"), containsString("text/plain"));
    }

    @Test
    public void jsonContentType() {
        String object = "";
        //#json-content-type
        JsonNode json = Json.toJson(object);
        Result jsonResult = ok(json);
        //#json-content-type

        assertThat(jsonResult.header("Content-Type"), containsString("application/json"));
    }

    @Test
    public void customContentType() {
        //#custom-content-type
        Result htmlResult = ok("<h1>Hello World!</h1>").as("text/html");
        //#custom-content-type

        assertThat(htmlResult.header("Content-Type"), containsString("text/html"));
    }

    @Test
    public void contextContentType() {
        assertThat(call(new MockJavaAction() {
            //#context-content-type
            public Result index() {
                response().setContentType("text/html");
                return ok("<h1>Hello World!</h1>");
            }
            //#context-content-type
        }, fakeRequest()).header("Content-Type"), containsString("text/html"));
    }

    @Test
    public void responseHeaders() {
        Map<String, String> headers = call(new MockJavaAction() {
            //#response-headers
            public Result index() {
                response().setContentType("text/html");
                response().setHeader(CACHE_CONTROL, "max-age=3600");
                response().setHeader(ETAG, "xxx");
                return ok("<h1>Hello World!</h1>");
            }
            //#response-headers
        }, fakeRequest()).headers();
        assertThat(headers.get(CACHE_CONTROL), equalTo("max-age=3600"));
        assertThat(headers.get(ETAG), equalTo("xxx"));
    }

    @Test
    public void setCookie() {
        setContext(fakeRequest());
        //#set-cookie
        response().setCookie("theme", "blue");
        //#set-cookie
        Cookie cookie = response().cookies().iterator().next();
        assertThat(cookie.name(), equalTo("theme"));
        assertThat(cookie.value(), equalTo("blue"));
        removeContext();
    }

    @Test
    public void detailedSetCookie() {
        setContext(fakeRequest());
        //#detailed-set-cookie
        response().setCookie(
                "theme",        // name
                "blue",         // value
                3600,           // maximum age
                "/some/path",   // path
                ".example.com", // domain
                false,          // secure
                true            // http only
        );
        //#detailed-set-cookie
        Cookie cookie = response().cookies().iterator().next();
        assertThat(cookie.name(), equalTo("theme"));
        assertThat(cookie.value(), equalTo("blue"));
        assertThat(cookie.maxAge(), equalTo(3600));
        assertThat(cookie.path(), equalTo("/some/path"));
        assertThat(cookie.domain(), equalTo(".example.com"));
        assertThat(cookie.secure(), equalTo(false));
        assertThat(cookie.httpOnly(), equalTo(true));
        removeContext();
    }

    @Test
    public void discardCookie() {
        setContext(fakeRequest());
        //#discard-cookie
        response().discardCookie("theme");
        //#discard-cookie
        Cookie cookie = response().cookies().iterator().next();
        assertThat(cookie.name(), equalTo("theme"));
        assertThat(cookie.value(), equalTo(""));
        removeContext();
    }

    @Test
    public void charset() {
        assertThat(call(new MockJavaAction() {
                    //#charset
                    public Result index() {
                        response().setContentType("text/html; charset=iso-8859-1");
                        return ok("<h1>Hello World!</h1>", "iso-8859-1");
                    }
                    //#charset
                }, fakeRequest()).header("Content-Type"),
                equalTo("text/html; charset=iso-8859-1"));
    }

}
