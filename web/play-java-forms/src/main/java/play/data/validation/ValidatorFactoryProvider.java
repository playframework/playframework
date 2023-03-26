/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolverContext;
import org.springframework.context.i18n.LocaleContextHolder;
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
        new ParameterMessageInterpolator(
            supportedLocales, defaultLocale, new RequestAwareLocaleResolver(langs), false);

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

  static class RequestAwareLocaleResolver implements LocaleResolver {

    private Langs langs;

    public RequestAwareLocaleResolver(Langs langs) {
      this.langs = langs;
    }

    @Override
    public Locale resolve(LocaleResolverContext context) {
      Locale defaultLocale = context.getDefaultLocale();
      Locale requestLocale = LocaleContextHolder.getLocale();

      if (requestLocale == null) {
        return defaultLocale;
      }

      return langs.preferred(Collections.singleton(new Lang(requestLocale))).toLocale();
    }
  }
}
