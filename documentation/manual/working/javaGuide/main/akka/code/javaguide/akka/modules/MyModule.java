/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka.modules;
import javaguide.akka.ConfiguredActor;

//#binding
import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;

public class MyModule extends AbstractModule implements AkkaGuiceSupport {
    @Override
    protected void configure() {
        bindActor(ConfiguredActor.class, "configured-actor");
    }
}
//#binding
