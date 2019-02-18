/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka.factorymodules;

import javaguide.akka.*;

// #factorybinding
import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;

public class MyModule extends AbstractModule implements AkkaGuiceSupport {
  @Override
  protected void configure() {
    bindActor(ParentActor.class, "parent-actor");
    bindActorFactory(ConfiguredChildActor.class, ConfiguredChildActorProtocol.Factory.class);
  }
}
// #factorybinding
