/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka.factorymodules;

import javaguide.akka.*;

//#factorybinding
import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;

public class MyModule extends AbstractModule implements AkkaGuiceSupport {
    @Override
    protected void configure() {
        bindActor(ParentActor.class, "parent-actor");
        bindActorFactory(ConfiguredChildActor.class,
            ConfiguredChildActorProtocol.Factory.class);
    }
}
//#factorybinding
