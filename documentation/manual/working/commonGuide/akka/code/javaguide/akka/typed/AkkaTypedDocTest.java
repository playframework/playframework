/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.inject.Module;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import play.ApplicationLoader;
import play.Environment;
import play.inject.Injector;
import play.inject.guice.GuiceApplicationBuilder;

public final class AkkaTypedDocTest {
  @Test
  void runtime_DI_support_for_OO_style_typed_actors() {
    Module module = new javaguide.akka.typed.oo.AppModule();
    GuiceApplicationBuilder builder = new GuiceApplicationBuilder().bindings(module);
    Injector injector = builder.configure("my.config", "foo").injector();
    javaguide.akka.typed.oo.Main main = injector.instanceOf(javaguide.akka.typed.oo.Main.class);
    assertNotNull(main.helloActor);
    assertNotNull(main.configuredActor);
  }

  @Test
  void runtime_DI_support_for_multi_instance_OO_style_typed_actors() {
    Module module = new javaguide.akka.typed.oo.multi.AppModule();
    GuiceApplicationBuilder builder = new GuiceApplicationBuilder().bindings(module);
    Injector injector = builder.configure("my.config", "foo").injector();
    javaguide.akka.typed.oo.multi.Main main =
        injector.instanceOf(javaguide.akka.typed.oo.multi.Main.class);
    assertNotNull(main.helloActor1);
    assertNotNull(main.helloActor2);
    assertNotNull(main.configuredActor1);
    assertNotNull(main.configuredActor2);
  }

  @Test
  void compile_time_DI_without_support_works() {
    // A sanity-check of what compile-time DI looks like
    Environment environment = Environment.simple();
    ApplicationLoader.Context context =
        ApplicationLoader.create(environment, Collections.singletonMap("my.config", "foo"));
    javaguide.akka.typed.oo.Main main = new javaguide.akka.typed.oo.AppComponents(context).main;
    assertNotNull(main.helloActor);
    assertNotNull(main.configuredActor);
  }
}
