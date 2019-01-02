/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import play.Application;
import play.Environment;
import play.core.j.JavaContextComponents;
import play.data.*;
import play.data.format.Formatters;
import play.data.Task;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http.Context;
import play.mvc.Http.Cookie;
import play.mvc.Http.RequestBuilder;

import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for the Http class. This test is in the play-java project
 * because we want to use some of the play-java classes, e.g.
 * the GuiceApplicationBuilder.
 */
public class HttpFormsTest {

    private static Config addLangs(Environment environment) {
      Config langOverrides = ConfigFactory.parseString("play.i18n.langs = [\"en\", \"en-US\", \"fr\" ]");
      Config loaded = ConfigFactory.load(environment.classLoader());
      return langOverrides.withFallback(loaded);
    }

    private static void withApplication(Consumer<Application> r) {
        Application app = new GuiceApplicationBuilder()
          .withConfigLoader(HttpFormsTest::addLangs)
          .build();
        play.api.Play.start(app.asScala());
        try {
            r.accept(app);
        } finally {
            play.api.Play.stop(app.asScala());
        }
    }

    private JavaContextComponents contextComponents(Application app) {
        return app.injector().instanceOf(JavaContextComponents.class);
    }

    private <T> Form<T> copyFormWithoutRawData(final Form<T> formToCopy, final Application app) {
        return new Form<T>(formToCopy.name(), formToCopy.getBackedType(), null, formToCopy.errors(), formToCopy.value(),
            (Class[])null, app.injector().instanceOf(MessagesApi.class), app.injector().instanceOf(Formatters.class), app.injector().instanceOf(ValidatorFactory.class), app.injector().instanceOf(Config.class), formToCopy.lang().orElse(null));
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
            Context ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse french input with french formatter
            ctx.changeLang("fr");
            Form<Money> myForm = formFactory.form(Money.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            Money money = myForm.get();
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("1234567.89"));
            assertThat(copyFormWithoutRawData(myForm, app).field("amount").value().get()).isEqualTo("1 234 567,89");
            // Parse french input with english formatter
            ctx.changeLang("en");
            myForm = formFactory.form(Money.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            money = myForm.get();
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("123456789"));
            assertThat(copyFormWithoutRawData(myForm, app).field("amount").value().get()).isEqualTo("123,456,789");

            // Prepare Request and Context with english number
            data = new HashMap<>();
            data.put("amount", "1234567.89");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse english input with french formatter
            ctx.changeLang("fr");
            myForm = formFactory.form(Money.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            money = myForm.get();
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("1234567"));
            assertThat(copyFormWithoutRawData(myForm, app).field("amount").value().get()).isEqualTo("1 234 567");
            // Parse english input with english formatter
            ctx.changeLang("en");
            myForm = formFactory.form(Money.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            money = myForm.get();
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("1234567.89"));
            assertThat(copyFormWithoutRawData(myForm, app).field("amount").value().get()).isEqualTo("1,234,567.89");

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
            ValidatorFactory validatorFactory = app.injector().instanceOf(ValidatorFactory.class);
            Config config = app.injector().instanceOf(Config.class);

            Lang lang = messagesApi.preferred(new RequestBuilder().build()).lang();

            List<String> msgs = new ArrayList<>();
            msgs.add("error.generalcustomerror");
            msgs.add("error.custom");
            List<Object> args = new ArrayList<>();
            args.add("error.customarg");
            List<ValidationError> errors = new ArrayList<>();
            errors.add(new ValidationError("foo", msgs, args));

            Form<Money> form = new Form<>(null, Money.class, new HashMap<>(), errors, Optional.empty(), null, messagesApi, formatters, validatorFactory, config, lang);

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
            Context ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse date input with pattern from the default messages file
            Form<Birthday> myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            Birthday birthday = myForm.get();
            assertThat(copyFormWithoutRawData(myForm, app).field("date").value().get()).isEqualTo("03/10/1986");
            assertThat(birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(1986, 10, 3));

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("date", "16.2.2001");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse french date input with pattern from the french messages file
            ctx.changeLang("fr");
            myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            birthday = myForm.get();
            assertThat(copyFormWithoutRawData(myForm, app).field("date").value().get()).isEqualTo("16.02.2001");
            assertThat(birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(2001, 2, 16));

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("date", "8-31-1950");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse english date input with pattern from the en-US messages file
            ctx.changeLang("en-US");
            myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            birthday = myForm.get();
            assertThat(copyFormWithoutRawData(myForm, app).field("date").value().get()).isEqualTo("08-31-1950");
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
            Context ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse date input with pattern from Play's default messages file
            Form<Birthday> myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            Birthday birthday = myForm.get();
            assertThat(copyFormWithoutRawData(myForm, app).field("alternativeDate").value().get()).isEqualTo("1982-05-07");
            assertThat(birthday.getAlternativeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(1982, 5, 7));

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("alternativeDate", "10_4_2005");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse french date input with pattern from the french messages file
            ctx.changeLang("fr");
            myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            birthday = myForm.get();
            assertThat(copyFormWithoutRawData(myForm, app).field("alternativeDate").value().get()).isEqualTo("10_04_2005");
            assertThat(birthday.getAlternativeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).isEqualTo(LocalDate.of(2005, 10, 4));

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("alternativeDate", "3/12/1962");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse english date input with pattern from the en-US messages file
            ctx.changeLang("en-US");
            myForm = formFactory.form(Birthday.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            birthday = myForm.get();
            assertThat(copyFormWithoutRawData(myForm, app).field("alternativeDate").value().get()).isEqualTo("03/12/1962");
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
            Context ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse date input with pattern from the default messages file
            Form<Task> myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isTrue();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            assertThat(myForm.error("dueDate").get().messages().size()).isEqualTo(2);
            assertThat(myForm.error("dueDate").get().messages().get(0)).isEqualTo("error.invalid");
            assertThat(myForm.error("dueDate").get().messages().get(1)).isEqualTo("error.invalid.java.util.Date");
            assertThat(myForm.error("dueDate").get().message()).isEqualTo("error.invalid.java.util.Date");

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("id", "1234567891");
            data.put("name", "peter");
            data.put("dueDate", "2009/11e/11");
            Cookie frCookie = new Cookie("PLAY_LANG", "fr", 0, "/", null, false, false, null);
            rb = new RequestBuilder().cookie(frCookie).uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse date input with pattern from the french messages file
            myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isTrue();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            assertThat(myForm.error("dueDate").get().messages().size()).isEqualTo(3);
            assertThat(myForm.error("dueDate").get().messages().get(0)).isEqualTo("error.invalid");
            assertThat(myForm.error("dueDate").get().messages().get(1)).isEqualTo("error.invalid.java.util.Date");
            assertThat(myForm.error("dueDate").get().messages().get(2)).isEqualTo("error.invalid.dueDate");
            assertThat(myForm.error("dueDate").get().message()).isEqualTo("error.invalid.dueDate");
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
            data.put("anotherZip", "1234");
            RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            Context ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse input with pattern from the default messages file
            Form<Task> myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("id", "1234567891");
            data.put("name", "peter");
            data.put("dueDate", "11/11/2009");
            data.put("zip", "567");
            data.put("anotherZip", "567");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse input with pattern from the french messages file
            ctx.changeLang("fr");
            myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isFalse();
            assertThat(myForm.hasGlobalErrors()).isFalse();

            // Prepare Request and Context
            data = new HashMap<>();
            data.put("id", "1234567891");
            data.put("name", "peter");
            data.put("dueDate", "11/11/2009");
            data.put("zip", "1234");
            data.put("anotherZip", "1234");
            rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
            ctx = new Context(rb, contextComponents(app));
            Context.current.set(ctx);
            // Parse WRONG input with pattern from the french messages file
            ctx.changeLang("fr");
            myForm = formFactory.form(Task.class).bindFromRequest();
            assertThat(myForm.hasErrors()).isTrue();
            assertThat(myForm.hasGlobalErrors()).isFalse();
            assertThat(myForm.error("zip").get().messages().size()).isEqualTo(1);
            assertThat(myForm.error("zip").get().message()).isEqualTo("error.i18nconstraint");
            assertThat(myForm.error("anotherZip").get().messages().size()).isEqualTo(1);
            assertThat(myForm.error("anotherZip").get().message()).isEqualTo("error.anotheri18nconstraint");
        });
    }

}
