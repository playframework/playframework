/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import com.typesafe.config.Config;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;

public class ValidatorsModule extends Module {
    @Override
    public List<Binding<?>> bindings(final Environment environment, final Config config) {
        return Arrays.asList(
                bindClass(ConstraintValidatorFactory.class).to(DefaultConstraintValidatorFactory.class),
                bindClass(Validator.class).toProvider(ValidatorProvider.class),
                bindClass(ValidatorFactory.class).toProvider(ValidatorFactoryProvider.class)
        );
    }
}
