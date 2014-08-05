/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.ebean;

import javax.inject.Singleton;
import play.api.Configuration;
import play.api.db.evolutions.DynamicEvolutions;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

/**
 * Injection module with default Ebean components.
 */
public class EbeanModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        if (configuration.underlying().getBoolean("play.modules.ebean.enabled")) {
            return seq(
                bind(DynamicEvolutions.class).to(EbeanDynamicEvolutions.class),
                bind(EbeanConfig.class).toProvider(DefaultEbeanConfig.EbeanConfigParser.class).in(Singleton.class)
            );
        } else {
            return seq();
        }
    }

}
