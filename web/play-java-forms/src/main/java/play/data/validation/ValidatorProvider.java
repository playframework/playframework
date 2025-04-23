/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import play.inject.ApplicationLifecycle;

/**
 * @deprecated Deprecated since 2.7.0. Use {@link ValidatorFactoryProvider} instead.
 */
@Deprecated
@Singleton
public class ValidatorProvider implements Provider<Validator> {

  private ValidatorFactory validatorFactory;

  @Inject
  public ValidatorProvider(
      ConstraintValidatorFactory constraintValidatorFactory, final ApplicationLifecycle lifecycle) {
    this.validatorFactory =
        Validation.byDefaultProvider()
            .configure()
            .constraintValidatorFactory(constraintValidatorFactory)
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();

    lifecycle.addStopHook(
        () -> {
          this.validatorFactory.close();
          return CompletableFuture.completedFuture(null);
        });
  }

  public Validator get() {
    return this.validatorFactory.getValidator();
  }
}
