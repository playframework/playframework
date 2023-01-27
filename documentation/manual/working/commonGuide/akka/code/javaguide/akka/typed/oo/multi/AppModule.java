/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed.oo.multi;

import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.Adapter;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import javaguide.akka.typed.oo.*;
import javax.inject.Inject;
import javax.inject.Provider;
import play.libs.akka.AkkaGuiceSupport;

public class AppModule extends AbstractModule implements AkkaGuiceSupport {
  @Override
  protected void configure() {
    bindHelloActor("hello-actor1");
    bindHelloActor("hello-actor2");
    bindConfiguredActor("configured-actor1");
    bindConfiguredActor("configured-actor2");
  }

  private void bindHelloActor(String name) {
    bind(new TypeLiteral<ActorRef<HelloActor.SayHello>>() {})
        .annotatedWith(Names.named(name))
        .toProvider(
            new Provider<ActorRef<HelloActor.SayHello>>() {
              @Inject ActorSystem actorSystem;

              @Override
              public ActorRef<HelloActor.SayHello> get() {
                return Adapter.spawn(actorSystem, HelloActor.create(), name);
              }
            })
        .asEagerSingleton();
  }

  private void bindConfiguredActor(String name) {
    bind(new TypeLiteral<ActorRef<ConfiguredActor.GetConfig>>() {})
        .annotatedWith(Names.named(name))
        .toProvider(
            new Provider<ActorRef<ConfiguredActor.GetConfig>>() {
              @Inject ActorSystem actorSystem;
              @Inject Config config;

              @Override
              public ActorRef<ConfiguredActor.GetConfig> get() {
                return Adapter.spawn(actorSystem, ConfiguredActor.create(config), name);
              }
            })
        .asEagerSingleton();
  }
}
