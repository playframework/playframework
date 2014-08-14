/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import javax.inject.Singleton;
import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

/**
 * Injection module with default DB components.
 */
public class DBModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        if (configuration.underlying().getBoolean("play.modules.db.enabled")) {
            return seq(bind(DBApi.class).to(DefaultDBApi.class));
        } else {
            return seq();
        }
    }

}
