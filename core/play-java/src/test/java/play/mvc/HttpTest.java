/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static play.mvc.Http.HeaderNames.ACCEPT_LANGUAGE;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import play.Application;
import play.Environment;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http.Cookie;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;

/**
 * Tests for the Http class. This test is in the play-java project because we want to use some of
 * the play-java classes, e.g. the GuiceApplicationBuilder.
 */
class HttpTest {

  /** Gets the PLAY_LANG cookie, or the last one if there is more than one */
  private String resultLangCookie(Result result, MessagesApi messagesApi) {
    String value = null;
    for (Cookie c : result.cookies()) {
      if (c.name().equals(messagesApi.langCookieName())) {
        value = c.value();
      }
    }
    return value;
  }

  private MessagesApi messagesApi(Application app) {
    return app.injector().instanceOf(MessagesApi.class);
  }

  private static Config addLangs(Environment environment) {
    Config langOverrides =
        ConfigFactory.parseString("play.i18n.langs = [\"en\", \"en-US\", \"fr\" ]");
    Config loaded = ConfigFactory.load(environment.classLoader());
    return langOverrides.withFallback(loaded);
  }

  private static void withApplication(Consumer<Application> r) {
    Application app = new GuiceApplicationBuilder().withConfigLoader(HttpTest::addLangs).build();
    play.api.Play.start(app.asScala());
    try {
      r.accept(app);
    } finally {
      play.api.Play.stop(app.asScala());
    }
  }

  @Test
  void testChangeLang() {
    withApplication(
        (app) -> {
          // Start off as 'en' with no cookie set
          Request req = new RequestBuilder().build();
          Result result = Results.ok();
          assertEquals("en", messagesApi(app).preferred(req).lang().code());
          assertNull(resultLangCookie(result, messagesApi(app)));
          // Change the language to 'en-US'
          Lang lang = Lang.forCode("en-US");
          req = new RequestBuilder().langCookie(lang, messagesApi(app)).build();
          result = result.withLang(lang, messagesApi(app));
          // The language and cookie should now be 'en-US'
          assertEquals("en-US", messagesApi(app).preferred(req).lang().code());
          assertEquals("en-US", resultLangCookie(result, messagesApi(app)));
          // The Messages instance uses the language which is set now into account
          assertEquals("Aloha", messagesApi(app).preferred(req).at("hello"));
        });
  }

  @Test
  void testMessagesOrder() {
    withApplication(
        (app) -> {
          RequestBuilder rb = new RequestBuilder().header(ACCEPT_LANGUAGE, "en-US");
          Request req = rb.build();
          // if no cookie is provided the lang order will have the accept language as the default
          assertEquals("en-US", messagesApi(app).preferred(req).lang().code());

          Lang fr = Lang.forCode("fr");
          rb = new RequestBuilder().langCookie(fr, messagesApi(app)).header(ACCEPT_LANGUAGE, "en");
          req = rb.build();

          // if no transient lang is provided the language order will be cookie > accept language
          assertEquals("fr", messagesApi(app).preferred(req).lang().code());

          // if a transient lang is set the order will be transient lang > cookie > accept language
          req = rb.build().withTransientLang(Lang.forCode("en-US"));
          assertEquals("en-US", messagesApi(app).preferred(req).lang().code());
        });
  }

  @Test
  void testChangeLangFailure() {
    withApplication(
        (app) -> {
          // Start off as 'en' with no cookie set
          Request req = new RequestBuilder().build();
          Result result = Results.ok();
          assertEquals("en", messagesApi(app).preferred(req).lang().code());
          assertNull(resultLangCookie(result, messagesApi(app)));
          Lang lang = Lang.forCode("en-NZ");
          req = new RequestBuilder().langCookie(lang, messagesApi(app)).build();
          result = result.withLang(lang, messagesApi(app));
          // Try to change the language to 'en-NZ' - which fails, the language should still be 'en'
          assertEquals("en", messagesApi(app).preferred(req).lang().code());
          // The cookie however will get set
          assertEquals("en-NZ", resultLangCookie(result, messagesApi(app)));
        });
  }

  @Test
  void testClearLang() {
    withApplication(
        (app) -> {
          // Set 'fr' as our initial language
          Lang lang = Lang.forCode("fr");
          Request req = new RequestBuilder().langCookie(lang, messagesApi(app)).build();
          Result result = Results.ok().withLang(lang, messagesApi(app));
          assertEquals("fr", messagesApi(app).preferred(req).lang().code());
          assertEquals("fr", resultLangCookie(result, messagesApi(app)));
          // Clear language
          result = result.withoutLang(messagesApi(app));
          // The cookie should be cleared
          assertEquals("", resultLangCookie(result, messagesApi(app)));
          // However the request is not effected by changing the result
          assertEquals("fr", messagesApi(app).preferred(req).lang().code());
        });
  }

  @Test
  void testSetTransientLang() {
    withApplication(
        (app) -> {
          Request req = new RequestBuilder().build();
          Result result = Results.ok();
          // Start off as 'en' with no cookie set
          assertEquals("en", messagesApi(app).preferred(req).lang().code());
          assertNull(resultLangCookie(result, messagesApi(app)));
          // Change the language to 'en-US'
          req = req.withTransientLang(Lang.forCode("en-US"));
          // The language should now be 'en-US', but the cookie mustn't be set
          assertEquals("en-US", messagesApi(app).preferred(req).lang().code());
          assertNull(resultLangCookie(result, messagesApi(app)));
          // The Messages instance uses the language which is set now into account
          assertEquals("Aloha", messagesApi(app).preferred(req).at("hello"));
        });
  }

  @Test
  void testSetTransientLangFailure() {
    withApplication(
        (app) -> {
          Request req = new RequestBuilder().build();
          Result result = Results.ok();
          // Start off as 'en' with no cookie set
          assertEquals("en", messagesApi(app).preferred(req).lang().code());
          assertNull(resultLangCookie(result, messagesApi(app)));
          // Try to change the language to 'en-NZ'
          req = req.withTransientLang(Lang.forCode("en-NZ"));
          // When trying to get the messages it does not work because en-NZ is not valid
          assertEquals("en", messagesApi(app).preferred(req).lang().code());
          // However if you access the transient lang directly you will see it was set
          assertEquals(Optional.of("en-NZ"), req.transientLang().map(Lang::code));
        });
  }

  @Test
  void testClearTransientLang() {
    withApplication(
        (app) -> {
          Lang lang = Lang.forCode("fr");
          RequestBuilder rb = new RequestBuilder().langCookie(lang, messagesApi(app));
          Result result = Results.ok().withLang(lang, messagesApi(app));
          // Start off as 'fr' with cookie set
          Request req = rb.build();
          assertEquals("fr", messagesApi(app).preferred(req).lang().code());
          assertEquals("fr", resultLangCookie(result, messagesApi(app)));
          // Change the language to 'en-US'
          lang = Lang.forCode("en-US");
          req = req.withTransientLang(lang);
          result = result.withLang(lang, messagesApi(app));
          // The language should now be 'en-US' and the cookie must be set again
          assertEquals("en-US", messagesApi(app).preferred(req).lang().code());
          assertEquals("en-US", resultLangCookie(result, messagesApi(app)));
          // Clear the language to the default for the current request and result
          req = req.withoutTransientLang();
          result = result.withoutLang(messagesApi(app));
          // The language should now be back to 'fr', and the cookie must be cleared
          assertEquals("fr", messagesApi(app).preferred(req).lang().code());
          assertEquals("", resultLangCookie(result, messagesApi(app)));
        });
  }

  @Test
  void testRequestImplLang() {
    withApplication(
        (app) -> {
          RequestBuilder rb = new RequestBuilder();
          Request req = rb.build();

          // Lets change the lang to something that is not the default
          req = req.withTransientLang(Lang.forCode("fr"));

          // Make sure the request did set that lang correctly
          assertEquals("fr", messagesApi(app).preferred(req).lang().code());

          // Now let's copy the request
          Request newReq = new Http.RequestImpl(req.asScala());

          // Make sure the new request correctly set its internal lang variable
          assertEquals("fr", messagesApi(app).preferred(newReq).lang().code());

          // Now change the lang on the new request to something not default
          newReq = newReq.withTransientLang(Lang.forCode("en-US"));

          // Make sure the new request correctly set its internal lang variable
          assertEquals("en-US", messagesApi(app).preferred(newReq).lang().code());
          assertEquals(Optional.of("en-US"), newReq.transientLang().map(Lang::code));

          // Also make sure the original request didn't change it's language
          assertEquals("fr", messagesApi(app).preferred(req).lang().code());
          assertEquals(Optional.of("fr"), req.transientLang().map(Lang::code));
        });
  }
}
