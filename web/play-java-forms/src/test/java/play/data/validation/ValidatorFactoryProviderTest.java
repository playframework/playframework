/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.typesafe.config.ConfigFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Size;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.messageinterpolation.HibernateMessageInterpolatorContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.api.Configuration;
import play.api.i18n.DefaultLangsProvider;
import play.data.validation.Constraints.ValidationPayload;
import play.i18n.Lang;
import play.i18n.Langs;
import play.inject.ApplicationLifecycle;
import play.libs.typedmap.TypedMap;

public class ValidatorFactoryProviderTest {

  private static final String GERMAN_MESSAGE = "Gr\u00f6\u00dfe muss zwischen 10 und 100 sein";
  private static final String ENGLISH_MESSAGE = "size must be between 10 and 100";
  private static final String SIZE_MESSAGE_TEMPLATE =
      "{jakarta.validation.constraints.Size.message}";

  // The fallback tests support the JVM's language but make another language the application
  // default, so falling back to the JVM default locale would give a different message on any
  // machine.
  private static final String JVM_LANGUAGE = Locale.getDefault().getLanguage();
  private static final String DEFAULT_LANGUAGE = "de".equals(JVM_LANGUAGE) ? "en" : "de";
  private static final String DEFAULT_LANGUAGE_MESSAGE =
      "de".equals(DEFAULT_LANGUAGE) ? GERMAN_MESSAGE : ENGLISH_MESSAGE;

  private final List<ValidatorFactory> validatorFactories = new ArrayList<>();
  private HibernateValidatorFactory validatorFactory;

  @Before
  public void createDefaultValidatorFactory() {
    validatorFactory = createValidatorFactory("de", "en");
  }

  @After
  public void closeValidatorFactories() {
    validatorFactories.forEach(ValidatorFactory::close);
  }

  @Test
  public void shouldUseDefaultLocaleWithoutPayload() {
    HibernateValidatorFactory factory = createValidatorFactory(DEFAULT_LANGUAGE, JVM_LANGUAGE);
    assertMessage(factory.getValidator(), DEFAULT_LANGUAGE_MESSAGE);
  }

  @Test
  public void shouldUsePayloadLocale() {
    assertMessage(validatorForLang(validatorFactory, Lang.forCode("en")), ENGLISH_MESSAGE);
  }

  @Test
  public void shouldUseDefaultLocaleWhenPayloadHasNoLang() {
    HibernateValidatorFactory factory = createValidatorFactory(DEFAULT_LANGUAGE, JVM_LANGUAGE);
    assertMessage(validatorForLang(factory, null), DEFAULT_LANGUAGE_MESSAGE);
  }

  @Test
  public void shouldUseDefaultLocaleForUnsupportedPayloadLang() {
    assertMessage(validatorForLang(validatorFactory, Lang.forCode("fr")), GERMAN_MESSAGE);
  }

  @Test
  public void shouldHonorExplicitLocaleInsteadOfPayloadLocale() {
    HibernateMessageInterpolatorContext context = mock(HibernateMessageInterpolatorContext.class);
    when(context.getConstraintValidatorPayload(ValidationPayload.class))
        .thenReturn(payload(Lang.forCode("en")));
    when(context.getMessageParameters()).thenReturn(Map.of());
    doReturn(sizeConstraintDescriptor()).when(context).getConstraintDescriptor();

    MessageInterpolator interpolator = validatorFactory.getMessageInterpolator();
    assertThat(interpolator.interpolate(SIZE_MESSAGE_TEMPLATE, context)).isEqualTo(ENGLISH_MESSAGE);
    assertThat(interpolator.interpolate(SIZE_MESSAGE_TEMPLATE, context, Locale.FRENCH))
        .isEqualTo("la taille doit \u00eatre comprise entre 10 et 100");
  }

  @Test
  public void shouldUseDefaultLocaleForNonHibernateContext() {
    MessageInterpolator.Context context = mock(MessageInterpolator.Context.class);
    doReturn(sizeConstraintDescriptor()).when(context).getConstraintDescriptor();

    assertThat(
            validatorFactory.getMessageInterpolator().interpolate(SIZE_MESSAGE_TEMPLATE, context))
        .isEqualTo(GERMAN_MESSAGE);
  }

  private ConstraintDescriptor<?> sizeConstraintDescriptor() {
    return validatorFactory
        .getValidator()
        .getConstraintsForClass(Note.class)
        .getConstraintsForProperty("value")
        .getConstraintDescriptors()
        .iterator()
        .next();
  }

  private HibernateValidatorFactory createValidatorFactory(String... langCodes) {
    Langs langs =
        new DefaultLangsProvider(
                new Configuration(
                    ConfigFactory.parseMap(Map.of("play.i18n.langs", List.of(langCodes)))))
            .get()
            .asJava();
    HibernateValidatorFactory factory =
        new ValidatorFactoryProvider(
                Validation.byDefaultProvider().configure().getDefaultConstraintValidatorFactory(),
                langs,
                mock(ApplicationLifecycle.class))
            .get()
            .unwrap(HibernateValidatorFactory.class);
    validatorFactories.add(factory);
    return factory;
  }

  private static Validator validatorForLang(HibernateValidatorFactory factory, Lang lang) {
    return factory.usingContext().constraintValidatorPayload(payload(lang)).getValidator();
  }

  private static ValidationPayload payload(Lang lang) {
    return new ValidationPayload(lang, null, TypedMap.empty(), ConfigFactory.empty());
  }

  private static void assertMessage(Validator validator, String expectedMessage) {
    assertThat(validator.validate(new Note()))
        .extracting(ConstraintViolation::getMessage)
        .containsExactly(expectedMessage);
  }

  public static class Note {
    @Size(min = 10, max = 100)
    public String value = "foo";
  }
}
