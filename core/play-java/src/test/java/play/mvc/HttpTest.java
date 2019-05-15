/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import play.Application;
import play.Environment;
import play.core.j.JavaContextComponents;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http.Context;
import play.mvc.Http.Cookie;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.HeaderNames.ACCEPT_LANGUAGE;

/**
 * Tests for the Http class. This test is in the play-java project because we want to use some of
 * the play-java classes, e.g. the GuiceApplicationBuilder.
 */
public class HttpTest {

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
  public void testChangeLang() {
    withApplication(
        (app) -> {
          // Start off as 'en' with no cookie set
          Request req = new RequestBuilder().build();
          Result result = Results.ok();
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en");
          assertThat(resultLangCookie(result, messagesApi(app))).isNull();
          // Change the language to 'en-US'
          Lang lang = Lang.forCode("en-US");
          req = new RequestBuilder().langCookie(lang, messagesApi(app)).build();
          result = result.withLang(lang, messagesApi(app));
          // The language and cookie should now be 'en-US'
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en-US");
          assertThat(resultLangCookie(result, messagesApi(app))).isEqualTo("en-US");
          // The Messages instance uses the language which is set now into account
          assertThat(messagesApi(app).preferred(req).at("hello")).isEqualTo("Aloha");
        });
  }

  @Test
  public void testMessagesOrder() {
    withApplication(
        (app) -> {
          RequestBuilder rb = new RequestBuilder().header(ACCEPT_LANGUAGE, "en-US");
          Request req = rb.build();
          // if no cookie is provided the lang order will have the accept language as the default
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en-US");

          Lang fr = Lang.forCode("fr");
          rb = new RequestBuilder().langCookie(fr, messagesApi(app)).header(ACCEPT_LANGUAGE, "en");
          req = rb.build();

          // if no transient lang is provided the language order will be cookie > accept language
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("fr");

          // if a transient lang is set the order will be transient lang > cookie > accept language
          req = rb.build().withTransientLang("en-US");
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en-US");
        });
  }

  @Test
  public void testChangeLangFailure() {
    withApplication(
        (app) -> {
          // Start off as 'en' with no cookie set
          Request req = new RequestBuilder().build();
          Result result = Results.ok();
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en");
          assertThat(resultLangCookie(result, messagesApi(app))).isNull();
          Lang lang = Lang.forCode("en-NZ");
          req = new RequestBuilder().langCookie(lang, messagesApi(app)).build();
          result = result.withLang(lang, messagesApi(app));
          // Try to change the language to 'en-NZ' - which fails, the language should still be 'en'
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en");
          // The cookie however will get set
          assertThat(resultLangCookie(result, messagesApi(app))).isEqualTo("en-NZ");
        });
  }

  @Test
  public void testClearLang() {
    withApplication(
        (app) -> {
          // Set 'fr' as our initial language
          Lang lang = Lang.forCode("fr");
          Request req = new RequestBuilder().langCookie(lang, messagesApi(app)).build();
          Result result = Results.ok().withLang(lang, messagesApi(app));
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("fr");
          assertThat(resultLangCookie(result, messagesApi(app))).isEqualTo("fr");
          // Clear language
          result = result.withoutLang(messagesApi(app));
          // The cookie should be cleared
          assertThat(resultLangCookie(result, messagesApi(app))).isEqualTo("");
          // However the request is not effected by changing the result
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("fr");
        });
  }

  @Test
  public void testSetTransientLang() {
    withApplication(
        (app) -> {
          Request req = new RequestBuilder().build();
          Result result = Results.ok();
          // Start off as 'en' with no cookie set
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en");
          assertThat(resultLangCookie(result, messagesApi(app))).isNull();
          // Change the language to 'en-US'
          req = req.withTransientLang(Lang.forCode("en-US"));
          // The language should now be 'en-US', but the cookie mustn't be set
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en-US");
          assertThat(resultLangCookie(result, messagesApi(app))).isNull();
          // The Messages instance uses the language which is set now into account
          assertThat(messagesApi(app).preferred(req).at("hello")).isEqualTo("Aloha");
        });
  }

  public void testSetTransientLangFailure() {
    withApplication(
        (app) -> {
          Request req = new RequestBuilder().build();
          Result result = Results.ok();
          // Start off as 'en' with no cookie set
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en");
          assertThat(resultLangCookie(result, messagesApi(app))).isNull();
          // Try to change the language to 'en-NZ'
          req = req.withTransientLang(Lang.forCode("en-NZ"));
          // When trying to get the messages it does not work because en-NZ is not valid
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en");
          // However if you access the transient lang directly you will see it was set
          assertThat(req.transientLang().map(l -> l.code())).isEqualTo(Optional.of("en-NZ"));
        });
  }

  @Test
  public void testClearTransientLang() {
    withApplication(
        (app) -> {
          Lang lang = Lang.forCode("fr");
          RequestBuilder rb = new RequestBuilder().langCookie(lang, messagesApi(app));
          Result result = Results.ok().withLang(lang, messagesApi(app));
          // Start off as 'fr' with cookie set
          Request req = rb.build();
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("fr");
          assertThat(resultLangCookie(result, messagesApi(app))).isEqualTo("fr");
          // Change the language to 'en-US'
          lang = Lang.forCode("en-US");
          req = req.withTransientLang(lang);
          result = result.withLang(lang, messagesApi(app));
          // The language should now be 'en-US' and the cookie must be set again
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("en-US");
          assertThat(resultLangCookie(result, messagesApi(app))).isEqualTo("en-US");
          // Clear the language to the default for the current request and result
          req = req.withoutTransientLang();
          result = result.withoutLang(messagesApi(app));
          // The language should now be back to 'fr', and the cookie must be cleared
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("fr");
          assertThat(resultLangCookie(result, messagesApi(app))).isEqualTo("");
        });
  }

  @Test
  public void testRequestImplLang() {
    withApplication(
        (app) -> {
          RequestBuilder rb = new RequestBuilder();
          Request req = rb.build();

          // Lets change the lang to something that is not the default
          req = req.withTransientLang(Lang.forCode("fr"));

          // Make sure the request did set that lang correctly
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("fr");

          // Now let's copy the request
          Request newReq = new Http.RequestImpl(req.asScala());

          // Make sure the new request correctly set its internal lang variable
          assertThat(messagesApi(app).preferred(newReq).lang().code()).isEqualTo("fr");

          // Now change the lang on the new request to something not default
          newReq = newReq.withTransientLang(Lang.forCode("en-US"));

          // Make sure the new request correctly set its internal lang variable
          assertThat(messagesApi(app).preferred(newReq).lang().code()).isEqualTo("en-US");
          assertThat(newReq.transientLang().map(l -> l.code())).isEqualTo(Optional.of("en-US"));

          // Also make sure the original request didn't change it's language
          assertThat(messagesApi(app).preferred(req).lang().code()).isEqualTo("fr");
          assertThat(req.transientLang().map(l -> l.code())).isEqualTo(Optional.of("fr"));
        });
  }

  @Test
  public void testWrappedCtxLang() {
    withApplication(
        (app) -> {
          JavaContextComponents contextComponents =
              app.injector().instanceOf(JavaContextComponents.class);

          Context ctx = new Context(new RequestBuilder(), contextComponents);

          // Lets change the lang to something that is not the default
          ctx.setTransientLang("fr");

          // Make sure the context did set that lang correctly
          assertThat(ctx.lang().code()).isEqualTo("fr");

          // Now let's copy the context - only with a new request set, the rest should stay the same
          Context newCtx = new Http.WrappedContext(ctx) {};

          // Make sure the new context correctly set its internal lang variable
          assertThat(newCtx.lang().code()).isEqualTo("fr");

          // Now change the lang on the new context to something not default
          newCtx.setTransientLang("en-US");

          // Make sure the new context correctly set its internal lang variable
          assertThat(newCtx.lang().code()).isEqualTo("en-US");
        });
  }

  @Test
  public void testTemplateMagicForJavaNoImplicitMessages() {
    withApplication(
        (app) -> {
          Context ctx =
              new Context(
                  new RequestBuilder(), app.injector().instanceOf(JavaContextComponents.class));

          ctx.changeLang("fr");

          try {
            Context.current.set(ctx);

            // Let's make sure french messages get returned from the context methods
            assertThat(Context.current().lang().code()).isEqualTo("fr");
            assertThat(Context.current().messages().at("bye")).isEqualTo("Au revoir!");

            Messages messages =
                messagesApi(app).preferred(Arrays.asList(new Lang(Locale.forLanguageTag("en-US"))));

            // Because the messages we pass to the view are not defined "implicit" the messages from
            // the context will be used
            assertThat(NoImplicitMessages.render(messages).toString()).isEqualTo("Au revoir!");
          } finally {
            Context.current.remove();
          }
        });
  }

  @Test
  public void testTemplateMagicForJavaImplicitMessages() {
    withApplication(
        (app) -> {
          Context ctx =
              new Context(
                  new RequestBuilder(), app.injector().instanceOf(JavaContextComponents.class));

          ctx.changeLang("fr");

          try {
            Context.current.set(ctx);

            // Let's make sure french messages get returned from the context methods
            assertThat(Context.current().lang().code()).isEqualTo("fr");
            assertThat(Context.current().messages().at("bye")).isEqualTo("Au revoir!");

            Messages messages =
                messagesApi(app).preferred(Arrays.asList(new Lang(Locale.forLanguageTag("en-US"))));

            // Because we pass our own (implicit) messages to the view now the implicit
            // PlayMagicForJava.implicitJavaMessages
            // should therefore have a lower weight and will not be used (resulting in the context
            // messages being ignored)
            assertThat(ImplicitMessages.render(messages).toString()).isEqualTo("See you!");
          } finally {
            Context.current.remove();
          }
        });
  }

  @Test
  public void testTemplateMagicForJavaNoImplicitLang() {
    withApplication(
        (app) -> {
          Context ctx =
              new Context(
                  new RequestBuilder(), app.injector().instanceOf(JavaContextComponents.class));

          ctx.changeLang("fr");

          try {
            Context.current.set(ctx);

            // Let's make sure the french lang gets returned from the context methods
            assertThat(Context.current().lang().code()).isEqualTo("fr");

            Lang lang = new Lang(Locale.forLanguageTag("en-US"));

            // Because the lang we pass to the view is not defined "implicit" the lang from the
            // context will be used
            assertThat(NoImplicitLang.render(lang).toString()).isEqualTo("fr");
          } finally {
            Context.current.remove();
          }
        });
  }

  @Test
  public void testTemplateMagicForJavaImplicitLang() {
    withApplication(
        (app) -> {
          Context ctx =
              new Context(
                  new RequestBuilder(), app.injector().instanceOf(JavaContextComponents.class));

          ctx.changeLang("fr");

          try {
            Context.current.set(ctx);

            // Let's make sure the french lang gets returned from the context methods
            assertThat(Context.current().lang().code()).isEqualTo("fr");

            Lang lang = new Lang(Locale.forLanguageTag("en-US"));

            // Because we pass our own (implicit) lang to the view now the implicit
            // PlayMagicForJava.implicitJavaLang
            // should therefore have a lower weight and will not be used (resulting in the context
            // lang being ignored)
            assertThat(ImplicitLang.render(lang).toString()).isEqualTo("en-US");
          } finally {
            Context.current.remove();
          }
        });
  }

  @Test
  public void testTemplateMagicForJavaNoImplicitRequest() {
    withApplication(
        (app) -> {
          Context ctx =
              new Context(
                  new RequestBuilder().cookie(Cookie.builder("location", "contextrequest").build()),
                  app.injector().instanceOf(JavaContextComponents.class));

          try {
            Context.current.set(ctx);

            // Let's make sure the request (and its cookie) is returned from the context methods
            assertThat(Context.current().request().getCookie("location").map(c -> c.value()))
                .isEqualTo(Optional.of("contextrequest"));

            Http.Request request =
                new RequestBuilder()
                    .cookie(Cookie.builder("location", "passedrequest").build())
                    .build();

            // Because the request we pass to the view is not defined "implicit" the request (and
            // therefore the cookie) from the context will be used
            assertThat(NoImplicitRequest.render(request).toString()).isEqualTo("contextrequest");
          } finally {
            Context.current.remove();
          }
        });
  }

  @Test
  public void testTemplateMagicForJavaImplicitRequest() {
    withApplication(
        (app) -> {
          Context ctx =
              new Context(
                  new RequestBuilder().cookie(Cookie.builder("location", "contextrequest").build()),
                  app.injector().instanceOf(JavaContextComponents.class));

          try {
            Context.current.set(ctx);

            // Let's make sure the request (and its cookie) is returned from the context methods
            assertThat(Context.current().request().getCookie("location").map(c -> c.value()))
                .isEqualTo(Optional.of("contextrequest"));

            Http.Request request =
                new RequestBuilder()
                    .cookie(Cookie.builder("location", "passedrequest").build())
                    .build();

            // Because we pass our own (implicit) request to the view now the implicit
            // PlayMagicForJava.requestHeader
            // should therefore have a lower weight and will not be used (resulting in the context
            // request being ignored)
            assertThat(ImplicitRequest.render(request).toString()).isEqualTo("passedrequest");
          } finally {
            Context.current.remove();
          }
        });
  }
}
