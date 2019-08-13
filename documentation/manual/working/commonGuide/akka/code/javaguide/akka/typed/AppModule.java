/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed;

// #oo-app-module
import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;

public class AppModule extends AbstractModule implements AkkaGuiceSupport {

  @Override
  protected void configure() {
    bindTypedActor(HelloActor.class, "hello-actor");
    bindTypedActor(ConfiguredActor.class, "configured-actor");
  }
}
// #oo-app-module
