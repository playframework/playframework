/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data.validation;

import play.inject.ApplicationLifecycle;
import play.inject.Injector;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validator;

/**
 * Java Components for Validator.
 */
public interface ValidatorsComponents {

    ApplicationLifecycle applicationLifecycle();

    default ConstraintValidatorFactory constraintValidatorFactory() {
        return new StaticConstraintValidatorFactory();
    }

    default Validator validator() {
        return new ValidatorProvider(constraintValidatorFactory(), applicationLifecycle()).get();
    }
}
