/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms;

import javaguide.testhelpers.MockJavaAction;
import org.junit.Test;
import play.core.j.JavaHandlerComponents;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.CSRF;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.crypto.CSRFTokenSigner;
import play.mvc.Result;
import play.test.WithApplication;

import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javaguide.testhelpers.MockJavaActionHelper.call;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.*;

public class JavaCsrf extends WithApplication {

  private CSRFTokenSigner tokenSigner() {
    return app.injector().instanceOf(CSRFTokenSigner.class);
  }

  @Test
  public void getToken() {
    String token = tokenSigner().generateSignedToken();
    String body =
        contentAsString(
            call(
                new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
                  @AddCSRFToken
                  public Result index() {
                    // #get-token
                    Optional<CSRF.Token> token = CSRF.getToken(request());
                    // #get-token
                    return ok(token.map(CSRF.Token::value).orElse(""));
                  }
                },
                fakeRequest("GET", "/").session("csrfToken", token),
                mat));

    assertTrue(tokenSigner().compareSignedTokens(body, token));
  }

  @Test
  public void templates() {
    CSRF.Token token = new CSRF.Token("csrfToken", tokenSigner().generateSignedToken());
    String body =
        contentAsString(
            call(
                new MockJavaAction(instanceOf(JavaHandlerComponents.class)) {
                  @AddCSRFToken
                  public Result index() {
                    return ok(javaguide.forms.html.csrf.render());
                  }
                },
                fakeRequest("GET", "/").session("csrfToken", token.value()),
                mat));

    Matcher matcher =
        Pattern.compile("action=\"/items\\?csrfToken=[a-f0-9]+-\\d+-([a-f0-9]+)\"").matcher(body);
    assertTrue(matcher.find());
    assertThat(matcher.group(1), equalTo(tokenSigner().extractSignedToken(token.value())));

    matcher = Pattern.compile("value=\"[a-f0-9]+-\\d+-([a-f0-9]+)\"").matcher(body);
    assertTrue(matcher.find());
    assertThat(matcher.group(1), equalTo(tokenSigner().extractSignedToken(token.value())));
  }

  @Test
  public void csrfCheck() {
    assertThat(
        call(
                new Controller1(instanceOf(JavaHandlerComponents.class)),
                fakeRequest("POST", "/")
                    .header("Cookie", "foo=bar")
                    .bodyForm(Collections.singletonMap("foo", "bar")),
                mat)
            .status(),
        equalTo(FORBIDDEN));
  }

  public static class Controller1 extends MockJavaAction {

    Controller1(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #csrf-check
    @RequireCSRFCheck
    public Result save() {
      // Handle body
      return ok();
    }
    // #csrf-check
  }

  @Test
  public void csrfAddToken() {
    assertThat(
        tokenSigner()
            .extractSignedToken(
                contentAsString(
                    call(
                        new Controller2(instanceOf(JavaHandlerComponents.class)),
                        fakeRequest("GET", "/"),
                        mat))),
        notNullValue());
  }

  public static class Controller2 extends MockJavaAction {

    Controller2(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #csrf-add-token
    @AddCSRFToken
    public Result get() {
      return ok(CSRF.getToken(request()).map(CSRF.Token::value).orElse("no token"));
    }
    // #csrf-add-token
  }
}
