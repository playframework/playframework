/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static play.inject.Bindings.bind;

import com.google.common.collect.ImmutableMap;
import com.google.inject.ConfigurationException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import play.Environment;
import play.Mode;
import play.inject.Binding;
import play.inject.Injector;
import play.inject.Module;
import scala.collection.Seq;

class GuiceInjectorBuilderTest {

  static Stream<Arguments> environmentTargets() {
    return Stream.of(
        arguments(named("Scala Env", new EnvironmentModule())),
        arguments(named("Java Env", new JavaEnvironmentModule())));
  }

  @ParameterizedTest
  @MethodSource("environmentTargets")
  void setEnvironment(play.api.inject.Module environmentModule) {
    ClassLoader classLoader = new URLClassLoader(new URL[0]);
    Environment env =
        new GuiceInjectorBuilder()
            .in(new Environment(new File("test"), classLoader, Mode.DEV))
            .bindings(environmentModule)
            .injector()
            .instanceOf(Environment.class);

    assertEquals(new File("test"), env.rootPath());
    assertEquals(Mode.DEV, env.mode());
    assertInstanceOf(classLoader.getClass(), env.classLoader());
  }

  @ParameterizedTest
  @MethodSource("environmentTargets")
  void setEnvironmentValues(play.api.inject.Module environmentModule) {
    ClassLoader classLoader = new URLClassLoader(new URL[0]);
    Environment env =
        new GuiceInjectorBuilder()
            .in(new File("test"))
            .in(Mode.DEV)
            .in(classLoader)
            .bindings(environmentModule)
            .injector()
            .instanceOf(Environment.class);

    assertEquals(new File("test"), env.rootPath());
    assertEquals(Mode.DEV, env.mode());
    assertInstanceOf(classLoader.getClass(), env.classLoader());
  }

  static Stream<Arguments> configurationTargets() {
    return Stream.of(
        arguments(named("Java", new JavaConfigurationModule())),
        arguments(named("Scala", new ConfigurationModule())));
  }

  @ParameterizedTest
  @MethodSource("configurationTargets")
  void setConfiguration(play.api.inject.Module configurationModule) {
    Config conf =
        new GuiceInjectorBuilder()
            .configure(ConfigFactory.parseMap(ImmutableMap.of("a", 1)))
            .configure(ImmutableMap.of("b", 2))
            .configure("c", 3)
            .configure("d.1", 4)
            .configure("d.2", 5)
            .bindings(configurationModule)
            .injector()
            .instanceOf(Config.class);

    assertEquals(4, conf.root().keySet().size());
    assertTrue(conf.root().keySet().containsAll(List.of("a", "b", "c", "d")));

    assertEquals(1, conf.getInt("a"));
    assertEquals(2, conf.getInt("b"));
    assertEquals(3, conf.getInt("c"));
    assertEquals(4, conf.getInt("d.1"));
    assertEquals(5, conf.getInt("d.2"));
  }

  static Stream<Arguments> bindingTargets() {
    return Stream.of(
        arguments(
            named("Scala Env", new EnvironmentModule()),
            named("Scala Config", new ConfigurationModule())),
        arguments(
            named("Java Env", new JavaEnvironmentModule()),
            named("Java Config", new JavaConfigurationModule())));
  }

  @ParameterizedTest
  @MethodSource("bindingTargets")
  void supportVariousBindings(
      play.api.inject.Module environmentModule, play.api.inject.Module configurationModule) {
    Injector injector =
        new GuiceInjectorBuilder()
            .bindings(environmentModule, configurationModule)
            .bindings(new AModule(), new BModule())
            .bindings(bind(C.class).to(C1.class), bind(D.class).toInstance(new D1()))
            .injector();

    assertInstanceOf(Environment.class, injector.instanceOf(Environment.class));
    assertInstanceOf(Config.class, injector.instanceOf(Config.class));
    assertInstanceOf(A1.class, injector.instanceOf(A.class));
    assertInstanceOf(B1.class, injector.instanceOf(B.class));
    assertInstanceOf(C1.class, injector.instanceOf(C.class));
    assertInstanceOf(D1.class, injector.instanceOf(D.class));
  }

  @Test
  void overrideBindings() {
    Injector injector =
        new GuiceInjectorBuilder()
            .bindings(new AModule())
            .bindings(bind(B.class).to(B1.class))
            .overrides(new A2Module())
            .overrides(bind(B.class).to(B2.class))
            .injector();

    assertInstanceOf(A2.class, injector.instanceOf(A.class));
    assertInstanceOf(B2.class, injector.instanceOf(B.class));
  }

  @Test
  void disableModules() {
    Injector injector =
        new GuiceInjectorBuilder()
            .bindings(new AModule(), new BModule())
            .bindings(bind(C.class).to(C1.class))
            .disable(AModule.class, CModule.class) // C won't be disabled
            .injector();

    assertInstanceOf(B1.class, injector.instanceOf(B.class));
    assertInstanceOf(C1.class, injector.instanceOf(C.class));
    assertThrows(ConfigurationException.class, () -> injector.instanceOf(A.class));
  }

  public static class EnvironmentModule extends play.api.inject.Module {
    @Override
    public Seq<play.api.inject.Binding<?>> bindings(
        play.api.Environment env, play.api.Configuration conf) {
      return seq(bind(Environment.class).toInstance(new Environment(env)));
    }
  }

  public static class ConfigurationModule extends play.api.inject.Module {
    @Override
    public Seq<play.api.inject.Binding<?>> bindings(
        play.api.Environment env, play.api.Configuration conf) {
      return seq(bind(Config.class).toInstance(conf.underlying()));
    }
  }

  public static class JavaEnvironmentModule extends Module {
    @Override
    public List<Binding<?>> bindings(Environment env, Config conf) {
      return Collections.singletonList(
          bindClass(Environment.class).toInstance(new Environment(env.asScala())));
    }
  }

  public static class JavaConfigurationModule extends Module {
    @Override
    public List<Binding<?>> bindings(Environment env, Config conf) {
      return Collections.singletonList(bindClass(Config.class).toInstance(conf));
    }
  }

  public interface A {}

  public static class A1 implements A {}

  public static class A2 implements A {}

  public static class AModule extends com.google.inject.AbstractModule {
    public void configure() {
      bind(A.class).to(A1.class);
    }
  }

  public static class A2Module extends com.google.inject.AbstractModule {
    public void configure() {
      bind(A.class).to(A2.class);
    }
  }

  public interface B {}

  public static class B1 implements B {}

  public static class B2 implements B {}

  public static class BModule extends com.google.inject.AbstractModule {
    public void configure() {
      bind(B.class).to(B1.class);
    }
  }

  public interface C {}

  public static class C1 implements C {}

  public static class CModule extends com.google.inject.AbstractModule {
    public void configure() {
      bind(C.class).to(C1.class);
    }
  }

  public interface D {}

  public static class D1 implements D {}
}
