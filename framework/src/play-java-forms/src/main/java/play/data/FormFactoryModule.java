/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

public class FormFactoryModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
            bind(FormFactory.class).toSelf()
        );
    }

}