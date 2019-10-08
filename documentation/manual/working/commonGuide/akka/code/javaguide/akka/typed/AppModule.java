/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed;

// #oo-app-module
import akka.actor.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.ActorRef;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.typesafe.config.Config;
import javax.inject.Inject;
import play.api.Configuration;
import play.api.libs.concurrent.AkkaGuiceSupport;

public class AppModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<ActorRef<HelloActor.SayHello>>() {})
        .toProvider(
            new Provider<ActorRef<HelloActor.SayHello>>() {
              @Inject ActorSystem actorSystem;

              @Override
              public ActorRef<HelloActor.SayHello> get() {
                return Adapter.spawn(actorSystem, HelloActor.create(), "hello-actor");
              }
            })
        .asEagerSingleton();
    bind(new TypeLiteral<ActorRef<ConfiguredActor.GetConfig>>() {})
        .toProvider(
            new Provider<ActorRef<ConfiguredActor.GetConfig>>() {
              @Inject ActorSystem actorSystem;
              @Inject Config config;

              @Override
              public ActorRef<ConfiguredActor.GetConfig> get() {
                return Adapter.spawn(
                    actorSystem, ConfiguredActor.create(config), "configured-actor");
              }
            })
        .asEagerSingleton();
  }
}
// #oo-app-module
