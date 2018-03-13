/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import play.inject.ApplicationLifecycle;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.ValidatorFactory;

/**
 * Java Components for Validator.
 */
public interface ValidatorsComponents {

    ApplicationLifecycle applicationLifecycle();

    default ConstraintValidatorFactory constraintValidatorFactory() {
        return new MappedConstraintValidatorFactory();
    }

    default ValidatorFactory validatorFactory() {
        return new ValidatorFactoryProvider(constraintValidatorFactory(), applicationLifecycle()).get();
    }
}
