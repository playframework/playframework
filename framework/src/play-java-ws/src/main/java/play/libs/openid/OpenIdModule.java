/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.openid;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

public class OpenIdModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        if (configuration.underlying().getBoolean("play.modules.openid.enabled")) {
            return seq(
                    bind(OpenIdClient.class).to(DefaultOpenIdClient.class)
            );
        } else {
            return seq();
        }
    }
}
