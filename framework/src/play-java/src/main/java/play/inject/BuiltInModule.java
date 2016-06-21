/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.inject;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.data.validation.DefaultConstraintValidatorFactory;
import play.data.validation.ValidatorProvider;
import play.libs.crypto.CSRFTokenSigner;
import play.libs.crypto.CookieSigner;
import play.libs.crypto.DefaultCSRFTokenSigner;
import play.libs.crypto.HMACSHA1CookieSigner;
import scala.collection.Seq;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validator;

public class BuiltInModule extends play.api.inject.Module {
    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
          bind(ApplicationLifecycle.class).to(DelegateApplicationLifecycle.class),
          bind(play.Configuration.class).toProvider(ConfigurationProvider.class),
          bind(ConstraintValidatorFactory.class).to(DefaultConstraintValidatorFactory.class),
          bind(Validator.class).toProvider(ValidatorProvider.class),
          bind(CSRFTokenSigner.class).to(DefaultCSRFTokenSigner.class),
          bind(CookieSigner.class).to(HMACSHA1CookieSigner.class)
        );
    }
}
