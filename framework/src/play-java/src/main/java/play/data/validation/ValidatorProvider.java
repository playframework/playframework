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

@Singleton
public class ValidatorProvider implements Provider<Validator> {

    private ConstraintValidatorFactory constraintValidatorFactory;

    @Inject
    public ValidatorProvider(ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
    }

    public Validator get() {
        return Validation.buildDefaultValidatorFactory().usingContext()
            .constraintValidatorFactory(constraintValidatorFactory)
            .getValidator();
    }

}