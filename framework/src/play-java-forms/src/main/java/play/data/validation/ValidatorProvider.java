/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data.validation;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.Validator;
import javax.validation.Validation;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ValidatorFactory;

@Singleton
public class ValidatorProvider implements Provider<Validator> {

    private ValidatorFactory validatorFactory;

    @Inject
    public ValidatorProvider(ConstraintValidatorFactory constraintValidatorFactory) {
        this.validatorFactory = Validation.byDefaultProvider().configure()
                .constraintValidatorFactory(constraintValidatorFactory)
                .buildValidatorFactory();
    }

    public Validator get() {
        return this.validatorFactory.getValidator();
    }

}