/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.typesafe.config.ConfigFactory;
import play.Application;
import play.Configuration;
import play.Environment;
import play.data.Birthday;
import play.data.models.Task;
import play.data.Form;
import play.data.FormFactory;
import play.data.Formats;
import play.data.Money;
import play.data.format.Formatters;
import play.data.validation.ValidationError;
import play.i18n.MessagesApi;
import play.Play;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http.*;
import org.junit.Test;

import javax.validation.Validator;

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
            // ctx.messages() takes the language which is set now into account
            assertThat(ctx.messages().at("hello")).isEqualTo("Aloha");
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
            // ctx.messages() takes the language which is set now into account
            assertThat(ctx.messages().at("hello")).isEqualTo("Aloha");
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
            Formatters formatters = app.injector().instanceOf(Formatters.class);

            // Register Formatter
            formatters.register(BigDecimal.class, new Formats.AnnotationCurrencyFormatter());

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

            // Clean up (Actually not really necassary because formatters are not global anyway ;-)
            formatters.conversion.removeConvertible(BigDecimal.class, String.class); // removes print conversion
            formatters.conversion.removeConvertible(String.class, BigDecimal.class); // removes parse conversion
        });
    }

    @Test
    public void testLangErrorsAsJson() {
        withApplication((app) -> {
            MessagesApi messagesApi = app.injector().instanceOf(MessagesApi.class);
            Formatters formatters = app.injector().instanceOf(Formatters.class);
            Validator validator = app.injector().instanceOf(Validator.class);

            RequestBuilder rb = new RequestBuilder();
            Context ctx = new Context(rb);
            Context.current.set(ctx);

            List<Object> args = new ArrayList<>();
            args.add("error.customarg");
            List<ValidationError> error = new ArrayList<>();
            error.add(new ValidationError("key", "error.custom", args));
            Map<String,List<ValidationError>> errors = new HashMap<>();
            errors.put("foo", error);
            Form form = new Form(null, Money.class, new HashMap<>(), errors, Optional.empty(), messagesApi, formatters, validator);

            assertThat(form.errorsAsJson().get("foo").toString()).isEqualTo("[\"It looks like something was not correct\"]");
        });
    }

    @Test
    public void testLangAnnotationDateDataBinder() {
        withApplication((app) -> {
            FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

            // Prepare Request and Context
            Map<String, String> data = new HashMap<>();
            data.put("date", "3/10/1986");
            RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            Context ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse date input with pattern from the default messages file
            Form<Birthday> myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            Birthday birthday = myForm.get();
            assertThat(myForm.field("date").value()).isEqualTo("03/10/1986");
            assertThat(birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(1986, 10, 3));

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("date", "16.2.2001");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse french date input with pattern from the french messages file
            ctx.changeLang("fr");
            myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            birthday = myForm.get();
            assertThat(myForm.field("date").value()).isEqualTo("16.02.2001");
            assertThat(birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(2001, 2, 16));

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("date", "8-31-1950");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse english date input with pattern from the en-US messages file
            ctx.changeLang("en-US");
            myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            birthday = myForm.get();
            assertThat(myForm.field("date").value()).isEqualTo("08-31-1950");
            assertThat(birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(1950, 8, 31));
        });
    }

    @Test
    public void testLangDateDataBinder() {
        withApplication((app) -> {
            FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

            // Prepare Request and Context
            Map<String, String> data = new HashMap<>();
            data.put("alternativeDate", "1982-5-7");
            RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            Context ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse date input with pattern from Play's default messages file
            Form<Birthday> myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            Birthday birthday = myForm.get();
            assertThat(myForm.field("alternativeDate").value()).isEqualTo("1982-05-07");
            assertThat(birthday.getAlternativeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(1982, 5, 7));

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("alternativeDate", "10_4_2005");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse french date input with pattern from the french messages file
            ctx.changeLang("fr");
            myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            birthday = myForm.get();
            assertThat(myForm.field("alternativeDate").value()).isEqualTo("10_04_2005");
            assertThat(birthday.getAlternativeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(2005, 10, 4));

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("alternativeDate", "3/12/1962");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse english date input with pattern from the en-US messages file
            ctx.changeLang("en-US");
            myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            birthday = myForm.get();
            assertThat(myForm.field("alternativeDate").value()).isEqualTo("03/12/1962");
            assertThat(birthday.getAlternativeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(1962, 12, 3));
        });
    }

    @Test
    public void testInvalidMessages() {
        withApplication((app) -> {
            FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

            // Prepare Request and Context
            Map<String, String> data = new HashMap<>();
            data.put("id", "1234567891");
            data.put("name", "peter");
            data.put("dueDate", "2009/11e/11");
            RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            Context ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse date input with pattern from the default messages file
            Form<Task> myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isTrue();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            assertThat(myForm.error("dueDate").messages().size()).isEqualTo(2);
            assertThat(myForm.error("dueDate").messages().get(0)).isEqualTo("error.invalid");
            assertThat(myForm.error("dueDate").messages().get(1)).isEqualTo("error.invalid.java.util.Date");
            assertThat(myForm.error("dueDate").message()).isEqualTo("error.invalid.java.util.Date");

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("id", "1234567891");
            data.put("name", "peter");
            data.put("dueDate", "2009/11e/11");
            Cookie frCookie = new Cookie("PLAY_LANG", "fr", null, "/", null, false, false);
            rb = new RequestBuilder().cookie(frCookie).uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse date input with pattern from the french messages file
            myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isTrue();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            assertThat(myForm.error("dueDate").messages().size()).isEqualTo(3);
            assertThat(myForm.error("dueDate").messages().get(0)).isEqualTo("error.invalid");
            assertThat(myForm.error("dueDate").messages().get(1)).isEqualTo("error.invalid.java.util.Date");
            assertThat(myForm.error("dueDate").messages().get(2)).isEqualTo("error.invalid.dueDate");
            assertThat(myForm.error("dueDate").message()).isEqualTo("error.invalid.dueDate");
        });
    }

    @Test
    public void testConstraintWithInjectedMessagesApi() {
        withApplication((app) -> {
            FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

            // Prepare Request and Context
            Map<String, String> data = new HashMap<>();
            data.put("id", "1234567891");
            data.put("name", "peter");
            data.put("dueDate", "11/11/2009");
            data.put("zip", "1234");
            RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            Context ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse input with pattern from the default messages file
            Form<Task> myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("id", "1234567891");
            data.put("name", "peter");
            data.put("dueDate", "11/11/2009");
            data.put("zip", "567");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse input with pattern from the french messages file
            ctx.changeLang("fr");
            myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("id", "1234567891");
            data.put("name", "peter");
            data.put("dueDate", "11/11/2009");
            data.put("zip", "1234");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb);
            Context.current.set(ctx);
            // Parse WRONG input with pattern from the french messages file
            ctx.changeLang("fr");
            myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isTrue();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            myForm.data().clear();
            assertThat(myForm.error("zip").messages().size()).isEqualTo(1);
            assertThat(myForm.error("zip").message()).isEqualTo("error.i18nconstraint");
        });
    }

}
