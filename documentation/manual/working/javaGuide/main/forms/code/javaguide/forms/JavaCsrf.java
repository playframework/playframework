/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.forms;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import play.Application;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.CSRFFilter;
import play.filters.csrf.RequireCSRFCheck;
import play.filters.csrf.CSRF;
import play.libs.Crypto;
import play.mvc.Result;
import play.test.WithApplication;

import static play.test.Helpers.*;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import javaguide.forms.html.form;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Optional;

public class JavaCsrf extends WithApplication {

    public Crypto crypto() {
      return app.injector().instanceOf(Crypto.class);
    }

    @Test
    public void getToken() {
        String token = crypto().generateSignedToken();
        String body = contentAsString(MockJavaActionHelper.call(new MockJavaAction() {
            public Result index() {
                //#get-token
                Optional<CSRF.Token> token = CSRF.getToken(request());
                //#get-token
                return ok(token.map(CSRF.Token::value).orElse(""));
            }
        }, fakeRequest("GET", "/").session("csrfToken", token)));

        assertTrue(crypto().compareSignedTokens(body, token));
    }

    @Test
    public void templates() {
        String token = crypto().generateSignedToken();
        String body = contentAsString(MockJavaActionHelper.call(new MockJavaAction() {
            public Result index() {
                return ok(javaguide.forms.html.csrf.render());
            }
        }, fakeRequest("GET", "/").session("csrfToken", token)));

        Matcher matcher = Pattern.compile("action=\"/items\\?csrfToken=[a-f0-9]+-\\d+-([a-f0-9]+)\"")
                .matcher(body);
        assertTrue(matcher.find());
        assertThat(matcher.group(1), equalTo(crypto().extractSignedToken(token)));

        matcher = Pattern.compile("value=\"[a-f0-9]+-\\d+-([a-f0-9]+)\"")
                .matcher(body);
        assertTrue(matcher.find());
        assertThat(matcher.group(1), equalTo(crypto().extractSignedToken(token)));
    }

    @Test
    public void csrfCheck() {
        assertThat(MockJavaActionHelper.call(new Controller1(), fakeRequest("POST", "/")
                .header(CONTENT_TYPE, "application/x-www-form-urlencoded")).status(), equalTo(FORBIDDEN));
    }

    public static class Controller1 extends MockJavaAction {
        //#csrf-check
        @RequireCSRFCheck
        public Result save() {
            // Handle body
            return ok();
        }
        //#csrf-check
    }

    @Test
    public void csrfAddToken() {
        assertThat(crypto().extractSignedToken(contentAsString(
                MockJavaActionHelper.call(new Controller2(), fakeRequest("GET", "/"))
        )), notNullValue());
    }

    public static class Controller2 extends MockJavaAction {
        //#csrf-add-token
        @AddCSRFToken
        public Result get() {
            return ok(form.render());
        }
        //#csrf-add-token
    }

}
