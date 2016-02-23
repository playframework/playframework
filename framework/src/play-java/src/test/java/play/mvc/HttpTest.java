/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.typesafe.config.ConfigFactory;
import play.Application;
import play.Configuration;
import play.Environment;
import play.data.Form;
import play.data.FormFactory;
import play.data.Formats;
import play.data.Money;
import play.data.format.Formatters;
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

    private static void withApplication(Consumer<Application> r) {
        Application app = new GuiceApplicationBuilder()
          .loadConfig(HttpTest::addLangs)
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
        withApplication((app) -> {
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
        withApplication((app) -> {
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
        withApplication((app) -> {
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
        withApplication((app) -> {
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
        withApplication((app) -> {
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

    @Test
    public void testLangDataBinder() {
        withApplication((app) -> {
            FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

            // Register Formatter
            Formatters.register(BigDecimal.class, new Formats.AnnotationCurrencyFormatter());

            // Prepare Request and Context with french number
            Map<String, String> data = new HashMap<>();
            data.put("amount", "1234567,89");
            RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            Context ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse french input with french formatter
            ctx.changeLang("fr");
            Form<Money> myForm = formFactory.form(Money.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            Money money = myForm.get();
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("1234567.89"));
            assertThat(myForm.field("amount").value()).isEqualTo("1 234 567,89");
            // Parse french input with english formatter
            ctx.changeLang("en");
            myForm = formFactory.form(Money.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            money = myForm.get();
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("123456789"));
            assertThat(myForm.field("amount").value()).isEqualTo("123,456,789");

            // Prepare Request and Context with english number
            data = new HashMap<>();
            data.put("amount", "1234567.89");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse english input with french formatter
            ctx.changeLang("fr");
            myForm = formFactory.form(Money.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            money = myForm.get();
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("1234567"));
            assertThat(myForm.field("amount").value()).isEqualTo("1 234 567");
            // Parse english input with english formatter
            ctx.changeLang("en");
            myForm = formFactory.form(Money.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            money = myForm.get();
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("1234567.89"));
            assertThat(myForm.field("amount").value()).isEqualTo("1,234,567.89");

            // Clean up
            Formatters.conversion.removeConvertible(BigDecimal.class, String.class); // removes print conversion
            Formatters.conversion.removeConvertible(String.class, BigDecimal.class); // removes parse conversion
        });
    }

}
