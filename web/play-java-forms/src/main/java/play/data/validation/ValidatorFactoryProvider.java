/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.hibernate.validator.messageinterpolation.HibernateMessageInterpolatorContext;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import play.data.validation.Constraints.ValidationPayload;
import play.i18n.Lang;
import play.i18n.Langs;
import play.inject.ApplicationLifecycle;

@Singleton
public class ValidatorFactoryProvider implements Provider<ValidatorFactory> {

  private ValidatorFactory validatorFactory;

  @Inject
  public ValidatorFactoryProvider(
      ConstraintValidatorFactory constraintValidatorFactory,
      Langs langs,
      final ApplicationLifecycle lifecycle) {

    Set<Locale> supportedLocales =
        langs.availables().stream().map(l -> l.locale()).collect(Collectors.toSet());
    Locale defaultLocale = langs.preferred(langs.availables()).toLocale();

    ParameterMessageInterpolator messageInterpolator =
        new ParameterMessageInterpolator(supportedLocales, defaultLocale, false) {
          @Override
          public String interpolate(String message, Context context) {
            final Lang requestLang =
                context
                    .unwrap(HibernateMessageInterpolatorContext.class)
                    .getConstraintValidatorPayload(ValidationPayload.class)
                    .getLang();
            return interpolate(
                message,
                context,
                requestLang == null || requestLang.toLocale() == null
                    ? defaultLocale
                    : langs
                        .preferred(Collections.singleton(new Lang(requestLang.toLocale())))
                        .toLocale());
          }
        };

    this.validatorFactory =
        Validation.byDefaultProvider()
            .configure()
            .constraintValidatorFactory(constraintValidatorFactory)
            .messageInterpolator(messageInterpolator)
            .buildValidatorFactory();

    lifecycle.addStopHook(
        () -> {
          this.validatorFactory.close();
          return CompletableFuture.completedFuture(null);
        });
  }

  public ValidatorFactory get() {
    return this.validatorFactory;
  }
}
