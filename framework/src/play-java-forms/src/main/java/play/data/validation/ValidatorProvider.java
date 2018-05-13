/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

@Singleton
public class ValidatorProvider implements Provider<Validator> {

    private ValidatorFactory validatorFactory;

    @Inject
    public ValidatorProvider(ConstraintValidatorFactory constraintValidatorFactory, final ApplicationLifecycle lifecycle) {
        this.validatorFactory = Validation.byDefaultProvider().configure()
                .constraintValidatorFactory(constraintValidatorFactory)
                .messageInterpolator(new ParameterMessageInterpolator())
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