/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed;

import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.Adapter;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import org.junit.Test;
import play.ApplicationLoader;
import play.Environment;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.akka.AkkaGuiceSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public final class AkkaTypedDocTest {
  @Test
  public void runtime_DI_support_for_OO_style_typed_actors() {
    GuiceApplicationBuilder builder = new GuiceApplicationBuilder().bindings(new AppModule());
    Main main = builder.configure("my.config", "foo").injector().instanceOf(Main.class);
    assertThat(main.helloActor, notNullValue());
    assertThat(main.configuredActor, notNullValue());
  }

  @Test
  public void runtime_DI_support_for_multi_instance_OO_style_typed_actors() {
    GuiceApplicationBuilder builder = new GuiceApplicationBuilder().bindings(new AppModule2());
    Main2 main = builder.configure("my.config", "foo").injector().instanceOf(Main2.class);
    assertThat(main.helloActor1, notNullValue());
    assertThat(main.helloActor2, notNullValue());
    assertThat(main.configuredActor1, notNullValue());
    assertThat(main.configuredActor2, notNullValue());
  }

  @Test
  public void compile_time_DI_without_support_works() {
    // A sanity-check of what compile-time DI looks like
    Environment environment = Environment.simple();
    ApplicationLoader.Context context =
        ApplicationLoader.create(environment, Collections.singletonMap("my.config", "foo"));
    AppComponents appComponents = new AppComponents(context);
    Main main = appComponents.main;
    assertThat(main.helloActor, notNullValue());
    assertThat(main.configuredActor, notNullValue());
  }

  private static class AppModule2 extends AbstractModule implements AkkaGuiceSupport {
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

  @Singleton
  public static class Main2 {
    public final ActorRef<HelloActor.SayHello> helloActor1;
    public final ActorRef<HelloActor.SayHello> helloActor2;
    public final ActorRef<ConfiguredActor.GetConfig> configuredActor1;
    public final ActorRef<ConfiguredActor.GetConfig> configuredActor2;

    @Inject
    public Main2(
        @Named("hello-actor1") ActorRef<HelloActor.SayHello> helloActor1,
        @Named("hello-actor2") ActorRef<HelloActor.SayHello> helloActor2,
        @Named("configured-actor1") ActorRef<ConfiguredActor.GetConfig> configuredActor1,
        @Named("configured-actor2") ActorRef<ConfiguredActor.GetConfig> configuredActor2) {
      this.helloActor1 = helloActor1;
      this.helloActor2 = helloActor2;
      this.configuredActor1 = configuredActor1;
      this.configuredActor2 = configuredActor2;
    }
  }
}
