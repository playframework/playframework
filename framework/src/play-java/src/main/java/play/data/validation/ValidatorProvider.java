/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data.validation;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.Validator;
import javax.validation.Validation;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ValidatorFactory;

import play.inject.ApplicationLifecycle;

@Singleton
public class ValidatorProvider implements Provider<Validator> {

    private ValidatorFactory validatorFactory;

    /**
     * @deprecated as of 2.5.11. Use ValidatorProvider(ConstraintValidatorFactory, ApplicationLifecycle)
     */
    @Deprecated
    public ValidatorProvider(ConstraintValidatorFactory constraintValidatorFactory) {
      this.validatorFactory = Validation.byDefaultProvider().configure()
          .constraintValidatorFactory(constraintValidatorFactory)
          .buildValidatorFactory();
    }

    @Inject
    public ValidatorProvider(ConstraintValidatorFactory constraintValidatorFactory, final ApplicationLifecycle lifecycle) {
        this.validatorFactory = Validation.byDefaultProvider().configure()
                .constraintValidatorFactory(constraintValidatorFactory)
                .buildValidatorFactory();

        lifecycle.addStopHook(() -> {
            this.validatorFactory.close();
            return CompletableFuture.completedFuture(null);
        });
    }

    public Validator get() {
        return this.validatorFactory.getValidator();
    }

}
