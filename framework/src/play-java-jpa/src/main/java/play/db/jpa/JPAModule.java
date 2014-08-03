/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Injection module with default JPA components.
 */
public class JPAModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        if (configuration.underlying().getBoolean("play.modules.jpa.enabled")) {
            return seq(
                bind(JPAApi.class).to(DefaultJPAApi.class),
                bind(JPAConfig.class).toProvider(DefaultJPAConfig.JPAConfigProvider.class).in(Singleton.class)
            );
        } else {
            return seq();
        }
    }

}
