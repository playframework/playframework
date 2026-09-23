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
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
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
        new ParameterMessageInterpolator(supportedLocales, defaultLocale, false);

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
