/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.validation;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import scala.collection.Seq;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validator;

public class ValidatorsModule extends play.api.inject.Module {
    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
                bind(ConstraintValidatorFactory.class).to(DefaultConstraintValidatorFactory.class),
                bind(Validator.class).toProvider(ValidatorProvider.class)
        );
    }
}
