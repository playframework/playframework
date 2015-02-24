/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject.guice;

import play.api.inject.guice.GuiceableModule;
import play.api.inject.guice.GuiceableModule$;
import play.libs.Scala;


public class Guiceable {

    public static GuiceableModule modules(com.google.inject.Module... modules) {
        return GuiceableModule$.MODULE$.fromGuiceModules(Scala.toSeq(modules));
    }

    public static GuiceableModule modules(play.api.inject.Module... modules) {
        return GuiceableModule$.MODULE$.fromPlayModules(Scala.toSeq(modules));
    }

    public static GuiceableModule bindings(play.api.inject.Binding... bindings) {
        return GuiceableModule$.MODULE$.fromPlayBindings(Scala.toSeq(bindings));
    }

    public static GuiceableModule module(Object module) {
        return GuiceableModule$.MODULE$.guiceable(module);
    }

}
