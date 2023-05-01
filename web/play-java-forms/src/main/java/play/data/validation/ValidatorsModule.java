/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import com.typesafe.config.Config;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

public class ValidatorsModule extends Module {
  @Override
  public List<Binding<?>> bindings(final Environment environment, final Config config) {
    return Arrays.asList(
        bindClass(ConstraintValidatorFactory.class).to(DefaultConstraintValidatorFactory.class),
        bindClass(Validator.class).toProvider(ValidatorProvider.class),
        bindClass(ValidatorFactory.class).toProvider(ValidatorFactoryProvider.class));
  }
}
