/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import play.i18n.Langs;
import play.inject.ApplicationLifecycle;

/** Java Components for Validator. */
public interface ValidatorsComponents {

  ApplicationLifecycle applicationLifecycle();

  Langs langs();

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
    return new ValidatorFactoryProvider(
            constraintValidatorFactory(), langs(), applicationLifecycle())
        .get();
  }
}
