/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import play.Application;
import play.Environment;
import play.api.i18n.DefaultLangs;
import play.data.*;
import play.data.format.Formatters;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.test.Helpers;

/**
 * Tests for the Http class. This test is in the play-java project because we want to use some of
 * the play-java classes, e.g. the GuiceApplicationBuilder.
 */
public class HttpFormsTest {

  private static Config addLangs(Environment environment) {
    Config langOverrides =
        ConfigFactory.parseString("play.i18n.langs = [\"en\", \"en-US\", \"fr\" ]");
    Config loaded = ConfigFactory.load(environment.classLoader());
    return langOverrides.withFallback(loaded);
  }

  private static void withApplication(Consumer<Application> r) {
    Application app =
        new GuiceApplicationBuilder().withConfigLoader(HttpFormsTest::addLangs).build();
    play.api.Play.start(app.asScala());
    try {
      r.accept(app);
    } finally {
      play.api.Play.stop(app.asScala());
    }
  }

  private <T> Form<T> copyFormWithoutRawData(final Form<T> formToCopy, final Application app) {
    return new Form<>(
        formToCopy.name(),
        formToCopy.getBackedType(),
        null,
        formToCopy.errors(),
        formToCopy.value(),
        null,
        app.injector().instanceOf(MessagesApi.class),
        app.injector().instanceOf(Formatters.class),
        app.injector().instanceOf(ValidatorFactory.class),
        app.injector().instanceOf(Config.class),
        formToCopy.lang().orElse(null));
  }

  @Test
  public void testLangDataBinder() {
    withApplication(
        (app) -> {
          FormFactory formFactory = app.injector().instanceOf(FormFactory.class);
          Formatters formatters = app.injector().instanceOf(Formatters.class);

          // Register Formatter
          formatters.register(BigDecimal.class, new Formats.AnnotationCurrencyFormatter());

          // Prepare Request with french number
          Map<String, String> data = new HashMap<>();
          data.put("amount", "1234567,89");
          RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse french input with french formatter
          Request req = rb.langCookie(Lang.forCode("fr"), Helpers.stubMessagesApi()).build();
          Form<Money> myForm = formFactory.form(Money.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          Money money = myForm.get();
          assertEquals(new BigDecimal("1234567.89"), money.getAmount());
          String amount = copyFormWithoutRawData(myForm, app).field("amount").value().get();
          assertEquals(
              amount.contains(" ") ? "1 234 567,89" : "1 234 567,89",
              amount); // Java 13+ uses different whitespaces
          // Parse french input with english formatter
          req = rb.langCookie(Lang.forCode("en"), Helpers.stubMessagesApi()).build();
          myForm = formFactory.form(Money.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          money = myForm.get();
          assertEquals(new BigDecimal("123456789"), money.getAmount());
          assertEquals(
              "123,456,789", copyFormWithoutRawData(myForm, app).field("amount").value().get());

          // Prepare Request with english number
          data = new HashMap<>();
          data.put("amount", "1234567.89");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse english input with french formatter
          req = rb.langCookie(Lang.forCode("fr"), Helpers.stubMessagesApi()).build();
          myForm = formFactory.form(Money.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          money = myForm.get();
          assertEquals(new BigDecimal("1234567"), money.getAmount());
          amount = copyFormWithoutRawData(myForm, app).field("amount").value().get();
          assertEquals(
              amount.contains(" ") ? "1 234 567" : "1 234 567",
              amount); // Java 13+ uses different whitespaces
          // Parse english input with english formatter
          req = rb.langCookie(Lang.forCode("en"), Helpers.stubMessagesApi()).build();
          myForm = formFactory.form(Money.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          money = myForm.get();
          assertEquals(new BigDecimal("1234567.89"), money.getAmount());
          assertEquals(
              "1,234,567.89", copyFormWithoutRawData(myForm, app).field("amount").value().get());

          // Clean up (Actually not really necassary because formatters are not global anyway ;-)
          formatters.conversion.removeConvertible(
              BigDecimal.class, String.class); // removes print conversion
          formatters.conversion.removeConvertible(
              String.class, BigDecimal.class); // removes parse conversion
        });
  }

  @Test
  public void testLangDataBinderTransient() {
    withApplication(
        (app) -> {
          FormFactory formFactory = app.injector().instanceOf(FormFactory.class);
          Formatters formatters = app.injector().instanceOf(Formatters.class);

          // Register Formatter
          formatters.register(BigDecimal.class, new Formats.AnnotationCurrencyFormatter());

          // Prepare Request with french number
          Map<String, String> data = new HashMap<>();
          data.put("amount", "1234567,89");
          RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse french input with french formatter
          Request req = rb.transientLang(Lang.forCode("fr")).build();
          Form<Money> myForm = formFactory.form(Money.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          Money money = myForm.get();
          assertEquals(new BigDecimal("1234567.89"), money.getAmount());
          String amount = copyFormWithoutRawData(myForm, app).field("amount").value().get();
          assertEquals(
              amount.contains(" ") ? "1 234 567,89" : "1 234 567,89",
              amount); // Java 13+ uses different whitespaces
          // Parse french input with english formatter
          req = rb.transientLang(Lang.forCode("en")).build();
          myForm = formFactory.form(Money.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          money = myForm.get();
          assertEquals(new BigDecimal("123456789"), money.getAmount());
          assertEquals(
              "123,456,789", copyFormWithoutRawData(myForm, app).field("amount").value().get());

          // Prepare Request with english number
          data = new HashMap<>();
          data.put("amount", "1234567.89");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse english input with french formatter
          req = rb.transientLang(Lang.forCode("fr")).build();
          myForm = formFactory.form(Money.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          money = myForm.get();
          assertEquals(new BigDecimal("1234567"), money.getAmount());
          amount = copyFormWithoutRawData(myForm, app).field("amount").value().get();
          String expectedAmountEN = amount.contains(" ") ? "1 234 567" : "1 234 567";
          assertEquals(expectedAmountEN, amount);

          // Parse english input with english formatter
          req = rb.transientLang(Lang.forCode("en")).build();
          myForm = formFactory.form(Money.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          money = myForm.get();
          assertEquals(new BigDecimal("1234567.89"), money.getAmount());
          assertEquals(
              "1,234,567.89", copyFormWithoutRawData(myForm, app).field("amount").value().get());

          // Clean up (Actually not really necassary because formatters are not global anyway ;-)
          formatters.conversion.removeConvertible(
              BigDecimal.class, String.class); // removes print conversion
          formatters.conversion.removeConvertible(
              String.class, BigDecimal.class); // removes parse conversion
        });
  }

  @Test
  public void testLangErrorsAsJson() {
    withApplication(
        (app) -> {
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

          Form<Money> form =
              new Form<>(
                  null,
                  Money.class,
                  new HashMap<>(),
                  errors,
                  Optional.empty(),
                  null,
                  messagesApi,
                  formatters,
                  validatorFactory,
                  config,
                  lang);

          assertEquals(
              "[\"It looks like something was not correct\"]",
              form.errorsAsJson().get("foo").toString());
        });
  }

  @Test
  public void testErrorsAsJsonWithEmptyMessages() {
    withApplication(
        (app) -> {
          // The messagesApi is empty
          MessagesApi emptyMessagesApi = play.test.Helpers.stubMessagesApi();
          Formatters formatters = app.injector().instanceOf(Formatters.class);
          ValidatorFactory validatorFactory = app.injector().instanceOf(ValidatorFactory.class);
          Config config = app.injector().instanceOf(Config.class);

          // The lang has to be build from an empty messagesApi
          final Lang lang =
              emptyMessagesApi.preferred(new DefaultLangs().asJava().availables()).lang();

          // Also the form should contain the empty messagesApi
          Form<Money> form =
              new Form<>(
                  null,
                  Money.class,
                  new HashMap<>(),
                  new ArrayList<>(),
                  Optional.empty(),
                  emptyMessagesApi,
                  formatters,
                  validatorFactory,
                  config);

          Map<String, String> data = new HashMap<>();
          data.put(
              "amount",
              "I am not a BigDecimal, I am a String that doesn't even represent a number! Binding to a "
                  + "BigDecimal will fail!");

          assertEquals(
              "{\"amount\":[\"error.invalid\"]}",
              form.bind(lang, new RequestBuilder().build().attrs(), data)
                  .errorsAsJson()
                  .toString());
        });
  }

  @Test
  public void testLangAnnotationDateDataBinder() {
    withApplication(
        (app) -> {
          FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

          // Prepare Request
          Map<String, String> data = new HashMap<>();
          data.put("date", "3/10/1986");
          RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse date input with pattern from the default messages file
          Request req = rb.build();
          Form<Birthday> myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          Birthday birthday = myForm.get();
          assertEquals(
              "03/10/1986", copyFormWithoutRawData(myForm, app).field("date").value().get());
          assertEquals(
              LocalDate.of(1986, 10, 3),
              birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

          // Prepare Request
          data = new HashMap<>();
          data.put("date", "16.2.2001");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse french date input with pattern from the french messages file
          req = rb.langCookie(Lang.forCode("fr"), Helpers.stubMessagesApi()).build();
          myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          birthday = myForm.get();
          assertEquals(
              "16.02.2001", copyFormWithoutRawData(myForm, app).field("date").value().get());
          assertEquals(
              LocalDate.of(2001, 2, 16),
              birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

          // Prepare Request
          data = new HashMap<>();
          data.put("date", "8-31-1950");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse english date input with pattern from the en-US messages file
          req = rb.langCookie(Lang.forCode("en-US"), Helpers.stubMessagesApi()).build();
          myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          birthday = myForm.get();
          assertEquals(
              "08-31-1950", copyFormWithoutRawData(myForm, app).field("date").value().get());
          assertEquals(
              LocalDate.of(1950, 8, 31),
              birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        });
  }

  @Test
  public void testLangAnnotationDateDataBinderTransient() {
    withApplication(
        (app) -> {
          FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

          // Prepare Request
          Map<String, String> data = new HashMap<>();
          data.put("date", "3/10/1986");
          RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse date input with pattern from the default messages file
          Request req = rb.build();
          Form<Birthday> myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          Birthday birthday = myForm.get();
          assertEquals(
              "03/10/1986", copyFormWithoutRawData(myForm, app).field("date").value().get());
          assertEquals(
              LocalDate.of(1986, 10, 3),
              birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

          // Prepare Request
          data = new HashMap<>();
          data.put("date", "16.2.2001");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse french date input with pattern from the french messages file
          req = rb.transientLang(Lang.forCode("fr")).build();
          myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          birthday = myForm.get();
          assertEquals(
              "16.02.2001", copyFormWithoutRawData(myForm, app).field("date").value().get());
          assertEquals(
              LocalDate.of(2001, 2, 16),
              birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

          // Prepare Request
          data = new HashMap<>();
          data.put("date", "8-31-1950");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse english date input with pattern from the en-US messages file
          req = rb.transientLang(Lang.forCode("en-US")).build();
          myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          birthday = myForm.get();
          assertEquals(
              "08-31-1950", copyFormWithoutRawData(myForm, app).field("date").value().get());
          assertEquals(
              LocalDate.of(1950, 8, 31),
              birthday.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        });
  }

  @Test
  public void testLangDateDataBinder() {
    withApplication(
        (app) -> {
          FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

          // Prepare Request
          Map<String, String> data = new HashMap<>();
          data.put("alternativeDate", "1982-5-7");
          RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse date input with pattern from Play's default messages file
          Request req = rb.build();
          Form<Birthday> myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          Birthday birthday = myForm.get();
          assertEquals(
              "1982-05-07",
              copyFormWithoutRawData(myForm, app).field("alternativeDate").value().get());
          assertEquals(
              LocalDate.of(1982, 5, 7),
              birthday
                  .getAlternativeDate()
                  .toInstant()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate());

          // Prepare Request
          data = new HashMap<>();
          data.put("alternativeDate", "10_4_2005");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse french date input with pattern from the french messages file
          req = rb.langCookie(Lang.forCode("fr"), Helpers.stubMessagesApi()).build();
          myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          birthday = myForm.get();
          assertEquals(
              "10_04_2005",
              copyFormWithoutRawData(myForm, app).field("alternativeDate").value().get());
          assertEquals(
              LocalDate.of(2005, 10, 4),
              birthday
                  .getAlternativeDate()
                  .toInstant()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate());

          // Prepare Request
          data = new HashMap<>();
          data.put("alternativeDate", "3/12/1962");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse english date input with pattern from the en-US messages file
          req = rb.langCookie(Lang.forCode("en-US"), Helpers.stubMessagesApi()).build();
          myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          birthday = myForm.get();
          assertEquals(
              "03/12/1962",
              copyFormWithoutRawData(myForm, app).field("alternativeDate").value().get());
          assertEquals(
              LocalDate.of(1962, 12, 3),
              birthday
                  .getAlternativeDate()
                  .toInstant()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate());
        });
  }

  @Test
  public void testLangDateDataBinderTransient() {
    withApplication(
        (app) -> {
          FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

          // Prepare Request
          Map<String, String> data = new HashMap<>();
          data.put("alternativeDate", "1982-5-7");
          RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse date input with pattern from Play's default messages file
          Request req = rb.build();
          Form<Birthday> myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          Birthday birthday = myForm.get();
          assertEquals(
              "1982-05-07",
              copyFormWithoutRawData(myForm, app).field("alternativeDate").value().get());
          assertEquals(
              LocalDate.of(1982, 5, 7),
              birthday
                  .getAlternativeDate()
                  .toInstant()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate());

          // Prepare Request
          data = new HashMap<>();
          data.put("alternativeDate", "10_4_2005");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse french date input with pattern from the french messages file
          req = rb.transientLang(Lang.forCode("fr")).build();
          myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          birthday = myForm.get();
          assertEquals(
              "10_04_2005",
              copyFormWithoutRawData(myForm, app).field("alternativeDate").value().get());
          assertEquals(
              LocalDate.of(2005, 10, 4),
              birthday
                  .getAlternativeDate()
                  .toInstant()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate());

          // Prepare Request
          data = new HashMap<>();
          data.put("alternativeDate", "3/12/1962");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse english date input with pattern from the en-US messages file
          req = rb.transientLang(Lang.forCode("en-US")).build();
          myForm = formFactory.form(Birthday.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          birthday = myForm.get();
          assertEquals(
              "03/12/1962",
              copyFormWithoutRawData(myForm, app).field("alternativeDate").value().get());
          assertEquals(
              LocalDate.of(1962, 12, 3),
              birthday
                  .getAlternativeDate()
                  .toInstant()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate());
        });
  }

  @Test
  public void testInvalidMessages() {
    withApplication(
        (app) -> {
          FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

          // Prepare Request
          Map<String, String> data = new HashMap<>();
          data.put("id", "1234567891");
          data.put("name", "peter");
          data.put("dueDate", "2009/11e/11");
          RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse date input with pattern from the default messages file
          Request req = rb.build();
          Form<Task> myForm = formFactory.form(Task.class).bindFromRequest(req);
          assertTrue(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          assertEquals(2, myForm.error("dueDate").get().messages().size());
          assertEquals("error.invalid", myForm.error("dueDate").get().messages().get(0));
          assertEquals(
              "error.invalid.java.util.Date", myForm.error("dueDate").get().messages().get(1));
          assertEquals("error.invalid.java.util.Date", myForm.error("dueDate").get().message());

          // Prepare Request
          data = new HashMap<>();
          data.put("id", "1234567891");
          data.put("name", "peter");
          data.put("dueDate", "2009/11e/11");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          req = rb.langCookie(Lang.forCode("fr"), Helpers.stubMessagesApi()).build();
          // Parse date input with pattern from the french messages file
          myForm = formFactory.form(Task.class).bindFromRequest(req);
          assertTrue(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          assertEquals(3, myForm.error("dueDate").get().messages().size());
          assertEquals("error.invalid", myForm.error("dueDate").get().messages().get(0));
          assertEquals(
              "error.invalid.java.util.Date", myForm.error("dueDate").get().messages().get(1));
          assertEquals("error.invalid.dueDate", myForm.error("dueDate").get().messages().get(2));
          assertEquals("error.invalid.dueDate", myForm.error("dueDate").get().message());
        });
  }

  @Test
  public void testConstraintWithInjectedMessagesApi() {
    withApplication(
        (app) -> {
          FormFactory formFactory = app.injector().instanceOf(FormFactory.class);

          // Prepare Request
          Map<String, String> data = new HashMap<>();
          data.put("id", "1234567891");
          data.put("name", "peter");
          data.put("dueDate", "11/11/2009");
          data.put("zip", "1234");
          data.put("anotherZip", "1234");
          RequestBuilder rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse input with pattern from the default messages file
          Request req = rb.build();
          Form<Task> myForm = formFactory.form(Task.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());

          // Prepare Request
          data = new HashMap<>();
          data.put("id", "1234567891");
          data.put("name", "peter");
          data.put("dueDate", "11/11/2009");
          data.put("zip", "567");
          data.put("anotherZip", "567");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse input with pattern from the french messages file
          req = rb.langCookie(Lang.forCode("fr"), Helpers.stubMessagesApi()).build();
          myForm = formFactory.form(Task.class).bindFromRequest(req);
          assertFalse(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());

          // Prepare Request
          data = new HashMap<>();
          data.put("id", "1234567891");
          data.put("name", "peter");
          data.put("dueDate", "11/11/2009");
          data.put("zip", "1234");
          data.put("anotherZip", "1234");
          rb = new RequestBuilder().uri("http://localhost/test").bodyForm(data);
          // Parse WRONG input with pattern from the french messages file
          req = rb.langCookie(Lang.forCode("fr"), Helpers.stubMessagesApi()).build();
          myForm = formFactory.form(Task.class).bindFromRequest(req);
          assertTrue(myForm.hasErrors());
          assertFalse(myForm.hasGlobalErrors());
          assertEquals(1, myForm.error("zip").get().messages().size());
          assertEquals("error.i18nconstraint", myForm.error("zip").get().message());
          assertEquals(1, myForm.error("anotherZip").get().messages().size());
          assertEquals("error.anotheri18nconstraint", myForm.error("anotherZip").get().message());
        });
  }
}
