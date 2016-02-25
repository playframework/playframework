/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.inject;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.data.validation.ValidatorProvider;
import play.data.validation.DefaultConstraintValidatorFactory;
import play.libs.Crypto;
import scala.collection.Seq;
import javax.validation.Validator;
import javax.validation.ConstraintValidatorFactory;

public class BuiltInModule extends play.api.inject.Module {
    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
          bind(ApplicationLifecycle.class).to(DelegateApplicationLifecycle.class),
          bind(play.Configuration.class).toProvider(ConfigurationProvider.class),
          bind(ConstraintValidatorFactory.class).to(DefaultConstraintValidatorFactory.class),
          bind(Validator.class).toProvider(ValidatorProvider.class),
          bind(Crypto.class).toSelf()
        );
    }
}
