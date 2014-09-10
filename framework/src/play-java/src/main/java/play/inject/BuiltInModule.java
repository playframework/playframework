/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.libs.Crypto;
import scala.collection.Seq;

public class BuiltInModule extends play.api.inject.Module {
    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
          bind(ApplicationLifecycle.class).to(DelegateApplicationLifecycle.class),
          bind(play.Configuration.class).toInstance(new play.Configuration(configuration)),
          bind(Crypto.class).toSelf()
        );
    }
}
