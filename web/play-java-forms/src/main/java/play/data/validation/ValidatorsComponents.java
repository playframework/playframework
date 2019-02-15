/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import play.inject.ApplicationLifecycle;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * Java Components for Validator.
 */
public interface ValidatorsComponents {

    ApplicationLifecycle applicationLifecycle();

    default ConstraintValidatorFactory constraintValidatorFactory() {
        return new MappedConstraintValidatorFactory();
    }

    /**
     * @deprecated Deprecated since 2.7.0. Use {@link #validatorFactory()} instead.
     */
    @Deprecated
    default Validator validator() {
        return new ValidatorProvider(constraintValidatorFactory(), applicationLifecycle()).get();
    }

    default ValidatorFactory validatorFactory() {
        return new ValidatorFactoryProvider(constraintValidatorFactory(), applicationLifecycle()).get();
    }
}
