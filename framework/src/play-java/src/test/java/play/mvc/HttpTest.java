/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import com.typesafe.config.ConfigFactory;
import play.Application;
import play.Configuration;
import play.Environment;
import play.Play;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http.*;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static play.Play.langCookieName;

/**
 * Tests for the Http class. This test is in the play-java project
 * because we want to use some of the play-java classes, e.g.
 * the GuiceApplicationBuilder.
 */
public class HttpTest {

    /** Gets the PLAY_LANG cookie, or the last one if there is more than one */
    private String responseLangCookie(Context ctx) {
        String value = null;
        for (Cookie c : ctx.response().cookies()) {
          if (c.name().equals(Play.langCookieName())) {
            value = c.value();
          }
        }
        return value;
    }

    private static Configuration addLangs(Environment environment) {
      Configuration langOverrides = new Configuration(ConfigFactory.parseString("play.i18n.langs = [\"en\", \"en-US\", \"fr\" ]"));
      Configuration loaded = Configuration.load(environment);
      return langOverrides.withFallback(loaded);
    }

    private static void withApplication(Runnable r) {
        Application app = new GuiceApplicationBuilder()
          .loadConfig(HttpTest::addLangs)
          .build();
        play.api.Play.start(app.getWrappedApplication());
        try {
            r.run();
        } finally {
            play.api.Play.stop(app.getWrappedApplication());
        }
    }

    @Test
    public void testChangeLang() {
        withApplication(() -> {
            Context ctx = new Context(new RequestBuilder());
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx)).isNull();
            // Change the language to 'en-US'
            assertThat(ctx.changeLang("en-US")).isTrue();
            // The language and cookie should now be 'en-US'
            assertThat(ctx.lang().code()).isEqualTo("en-US");
            assertThat(responseLangCookie(ctx)).isEqualTo("en-US");
        });
    }

    @Test
    public void testChangeLangFailure() {
        withApplication(() -> {
            Context ctx = new Context(new RequestBuilder());
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx)).isNull();
            // Try to change the language to 'en-NZ' - which will fail and return false
            assertThat(ctx.changeLang("en-NZ")).isFalse();
            // The language should still be 'en' and cookie should still be empty
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx)).isNull();
        });
    }

    @Test
    public void testClearLang() {
        withApplication(() -> {
            Context ctx = new Context(new RequestBuilder());
            // Set 'fr' as our initial language
            assertThat(ctx.changeLang("fr")).isTrue();
            assertThat(ctx.lang().code()).isEqualTo("fr");
            assertThat(responseLangCookie(ctx)).isEqualTo("fr");
            // Clear language
            ctx.clearLang();
            // The language should now be 'en' and the cookie should be null
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx)).isEqualTo("");
        });
    }

    @Test
    public void testSetTransientLang() {
        withApplication(() -> {
            Context ctx = new Context(new RequestBuilder());
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx)).isNull();
            // Change the language to 'en-US'
            ctx.setTransientLang("en-US");
            // The language should now be 'en-US', but the cookie mustn't be set
            assertThat(ctx.lang().code()).isEqualTo("en-US");
            assertThat(responseLangCookie(ctx)).isNull();
        });
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetTransientLangFailure() {
        withApplication(() -> {
            Context ctx = new Context(new RequestBuilder());
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("en");
            assertThat(responseLangCookie(ctx)).isNull();
            // Try to change the language to 'en-NZ' - which will throw an exception
            ctx.setTransientLang("en-NZ");
        });
    }

    @Test
    public void testClearTransientLang() {
        withApplication(() -> {
            Cookie frCookie = new Cookie("PLAY_LANG", "fr", null, "/", null, false, false);
            RequestBuilder rb = new RequestBuilder().cookie(frCookie);
            Context ctx = new Context(rb);
            // Start off as 'en' with no cookie set
            assertThat(ctx.lang().code()).isEqualTo("fr");
            assertThat(responseLangCookie(ctx)).isNull();
            // Change the language to 'en-US'
            ctx.setTransientLang("en-US");
            // The language should now be 'en-US', but the cookie mustn't be set
            assertThat(ctx.lang().code()).isEqualTo("en-US");
            assertThat(responseLangCookie(ctx)).isNull();
            // Clear the language to the default for the current request
            ctx.clearTransientLang();
            // The language should now be back to 'fr', and the cookie still mustn't be set
            assertThat(ctx.lang().code()).isEqualTo("fr");
            assertThat(responseLangCookie(ctx)).isNull();
        });
    }


}
