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
import javax.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import play.inject.ApplicationLifecycle;

@Singleton
public class ValidatorFactoryProvider implements Provider<ValidatorFactory> {

  private ValidatorFactory validatorFactory;

  @Inject
  public ValidatorFactoryProvider(
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

  public ValidatorFactory get() {
    return this.validatorFactory;
  }
}
