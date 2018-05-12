/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.util.function.Consumer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import play.Application;
import play.Environment;
import play.core.j.JavaContextComponents;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http.Context;
import play.mvc.Http.Cookie;
import play.mvc.Http.RequestBuilder;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for the Http class. This test is in the play-java project
 * because we want to use some of the play-java classes, e.g.
 * the GuiceApplicationBuilder.
 */
public class HttpTest {

    /** Gets the PLAY_LANG cookie, or the last one if there is more than one */
    private String responseLangCookie(Context ctx, MessagesApi messagesApi) {
        String value = null;
        for (Cookie c : ctx.response().cookies()) {
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
      Config langOverrides = ConfigFactory.parseString("play.i18n.langs = [\"en\", \"en-US\", \"fr\" ]");
      Config loaded = ConfigFactory.load(environment.classLoader());
      return langOverrides.withFallback(loaded);
    }

    private static void withApplication(Consumer<Application> r) {
        Application app = new GuiceApplicationBuilder()
          .withConfigLoader(HttpTest::addLangs)
          .build();
        play.api.Play.start(app.getWrappedApplication());
        try {
            r.accept(app);
        } finally {
            play.api.Play.stop(app.getWrappedApplication());
        }
    }

    @Test
    public void testChangeLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Change the language to 'en-US'
            assertThat(ctx.changeLang("en-US")).isTrue();
            // The language and cookie should now be 'en-US'
            assertThat(ctx.lang().code()).isEqualTo("en-US");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isEqualTo("en-US");
            // ctx.messages() takes the language which is set now into account
            assertThat(ctx.messages().at("hello")).isEqualTo("Aloha");
        });
    }

    @Test
    public void testMessagesOrder() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);
            Context ctx1 = new Context(new RequestBuilder().header(Http.HeaderNames.ACCEPT_LANGUAGE, "en-US"),
                    contextComponents);
            // if no cookie is provided the context lang order will have the accept language as the default lang
            assertThat(ctx1.messages().lang().code()).isEqualTo("en-US");

            Cookie cookie = Cookie.builder("PLAY_LANG", "fr").build();
            Context ctx2 = new Context(
                    new RequestBuilder().cookie(cookie).header(Http.HeaderNames.ACCEPT_LANGUAGE, "en"),
                    contextComponents);

            // if no context lang is provided the language order will be cookie > accept language
            assertThat(ctx2.messages().lang().code()).isEqualTo("fr");

            // if a context lang is set the language order will be context lang > cookie > accept language
            // Change the language to 'en-US'
            assertThat(ctx2.changeLang("en-US")).isTrue();
            // The messages language 'en-US'
            assertThat(ctx2.messages().lang().code()).isEqualTo("en-US");

            // check's that the order stays the same even when no cookie is changed
            // by using setTransientLang which will not set any cookie
            Context ctx3 = new Context(
                    new RequestBuilder().cookie(cookie).header(Http.HeaderNames.ACCEPT_LANGUAGE, "en"),
                    contextComponents);

            ctx3.setTransientLang("en-US");
            assertThat(ctx3.messages().lang().code()).isEqualTo("en-US");
        });
    }

    @Test
    public void testChangeLangFailure() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Try to change the language to 'en-NZ' - which will fail and return false
            assertThat(ctx.changeLang("en-NZ")).isFalse();
            // The language should still be 'en' and cookie should still be empty
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
        });
    }

    @Test
    public void testClearLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Set 'fr' as our initial language
            assertThat(ctx.changeLang("fr")).isTrue();
            assertThat(ctx.lang().code()).isEqualTo("fr");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isEqualTo("fr");
            // Clear language
            ctx.clearLang();
            // The language should now be 'en' and the cookie should be null
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isEqualTo("");
        });
    }

    @Test
    public void testSetTransientLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Change the language to 'en-US'
            ctx.setTransientLang("en-US");
            // The language should now be 'en-US', but the cookie mustn't be set
            assertThat(ctx.lang().code()).isEqualTo("en-US");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // ctx.messages() takes the language which is set now into account
            assertThat(ctx.messages().at("hello")).isEqualTo("Aloha");
        });
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetTransientLangFailure() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Context ctx = new Context(new RequestBuilder(), contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Try to change the language to 'en-NZ' - which will throw an exception
            ctx.setTransientLang("en-NZ");
        });
    }

    @Test
    public void testClearTransientLang() {
        withApplication((app) -> {
            JavaContextComponents contextComponents = app.injector().instanceOf(JavaContextComponents.class);

            Cookie frCookie = new Cookie("PLAY_LANG", "fr", null, "/", null, false, false, null);
            RequestBuilder rb = new RequestBuilder().cookie(frCookie);
            Context ctx = new Context(rb, contextComponents);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("fr");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Change the language to 'en-US'
            ctx.setTransientLang("en-US");
            // The language should now be 'en-US', but the cookie mustn't be set
            assertThat(ctx.lang().code()).isEqualTo("en-US");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
            // Clear the language to the default for the current request
            ctx.clearTransientLang();
            // The language should now be back to 'fr', and the cookie still mustn't be set
            assertThat(ctx.lang().code()).isEqualTo("fr");
            assertThat(responseLangCookie(ctx, messagesApi(app))).isNull();
        });
    }
}
