/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import com.typesafe.config.Config;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;

public class ValidatorsModule extends Module {
  @Override
  public List<Binding<?>> bindings(final Environment environment, final Config config) {
    return Arrays.asList(
        bindClass(ConstraintValidatorFactory.class).to(DefaultConstraintValidatorFactory.class),
        bindClass(Validator.class).toProvider(ValidatorProvider.class),
        bindClass(ValidatorFactory.class).toProvider(ValidatorFactoryProvider.class));
  }
}
