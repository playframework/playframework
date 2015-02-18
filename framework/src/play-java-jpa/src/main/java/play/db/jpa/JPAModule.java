/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import javax.inject.Singleton;
import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

/**
 * Injection module with default JPA components.
 */
public class JPAModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
            bind(JPAApi.class).toProvider(DefaultJPAApi.JPAApiProvider.class),
            bind(JPAConfig.class).toProvider(DefaultJPAConfig.JPAConfigProvider.class)
        );
    }

}
