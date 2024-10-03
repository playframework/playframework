/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
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
import org.junit.Test;
import play.Environment;
import play.Mode;
import play.inject.Binding;
import play.inject.Injector;
import play.inject.Module;
import scala.collection.Seq;

public class GuiceInjectorBuilderTest {

  @Test
  public void setEnvironmentWithScala() {
    setEnvironment(new EnvironmentModule());
  }

  @Test
  public void setEnvironmentWithJava() {
    setEnvironment(new JavaEnvironmentModule());
  }

  private void setEnvironment(play.api.inject.Module environmentModule) {
    ClassLoader classLoader = new URLClassLoader(new URL[0]);
    Environment env =
        new GuiceInjectorBuilder()
            .in(new Environment(new File("test"), classLoader, Mode.DEV))
            .bindings(environmentModule)
            .injector()
            .instanceOf(Environment.class);

    assertThat(env.rootPath()).isEqualTo(new File("test"));
    assertThat(env.mode()).isEqualTo(Mode.DEV);
    assertThat(env.classLoader()).isSameAs(classLoader);
  }

  @Test
  public void setEnvironmentValuesWithScala() {
    setEnvironmentValues(new EnvironmentModule());
  }

  @Test
  public void setEnvironmentValuesWithJava() {
    setEnvironmentValues(new JavaEnvironmentModule());
  }

  private void setEnvironmentValues(play.api.inject.Module environmentModule) {
    ClassLoader classLoader = new URLClassLoader(new URL[0]);
    Environment env =
        new GuiceInjectorBuilder()
            .in(new File("test"))
            .in(Mode.DEV)
            .in(classLoader)
            .bindings(environmentModule)
            .injector()
            .instanceOf(Environment.class);

    assertThat(env.rootPath()).isEqualTo(new File("test"));
    assertThat(env.mode()).isEqualTo(Mode.DEV);
    assertThat(env.classLoader()).isSameAs(classLoader);
  }

  @Test
  public void setConfigurationWithScala() {
    setConfiguration(new ConfigurationModule());
  }

  @Test
  public void setConfigurationWithJava() {
    setConfiguration(new JavaConfigurationModule());
  }

  private void setConfiguration(play.api.inject.Module configurationModule) {
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

    assertThat(conf.root().keySet()).hasSize(4);
    assertThat(conf.root().keySet()).contains("a", "b", "c", "d");

    assertThat(conf.getInt("a")).isEqualTo(1);
    assertThat(conf.getInt("b")).isEqualTo(2);
    assertThat(conf.getInt("c")).isEqualTo(3);
    assertThat(conf.getInt("d.1")).isEqualTo(4);
    assertThat(conf.getInt("d.2")).isEqualTo(5);
  }

  @Test
  public void supportVariousBindingsWithScala() {
    supportVariousBindings(new EnvironmentModule(), new ConfigurationModule());
  }

  @Test
  public void supportVariousBindingsWithJava() {
    supportVariousBindings(new JavaEnvironmentModule(), new JavaConfigurationModule());
  }

  private void supportVariousBindings(
      play.api.inject.Module environmentModule, play.api.inject.Module configurationModule) {
    Injector injector =
        new GuiceInjectorBuilder()
            .bindings(environmentModule, configurationModule)
            .bindings(new AModule(), new BModule())
            .bindings(bind(C.class).to(C1.class), bind(D.class).toInstance(new D1()))
            .injector();

    assertThat(injector.instanceOf(Environment.class)).isInstanceOf(Environment.class);
    assertThat(injector.instanceOf(Config.class)).isInstanceOf(Config.class);
    assertThat(injector.instanceOf(A.class)).isInstanceOf(A1.class);
    assertThat(injector.instanceOf(B.class)).isInstanceOf(B1.class);
    assertThat(injector.instanceOf(C.class)).isInstanceOf(C1.class);
    assertThat(injector.instanceOf(D.class)).isInstanceOf(D1.class);
  }

  @Test
  public void overrideBindings() {
    Injector injector =
        new GuiceInjectorBuilder()
            .bindings(new AModule())
            .bindings(bind(B.class).to(B1.class))
            .overrides(new A2Module())
            .overrides(bind(B.class).to(B2.class))
            .injector();

    assertThat(injector.instanceOf(A.class)).isInstanceOf(A2.class);
    assertThat(injector.instanceOf(B.class)).isInstanceOf(B2.class);
  }

  @Test
  public void disableModules() {
    Injector injector =
        new GuiceInjectorBuilder()
            .bindings(new AModule(), new BModule())
            .bindings(bind(C.class).to(C1.class))
            .disable(AModule.class, CModule.class) // C won't be disabled
            .injector();

    assertThat(injector.instanceOf(B.class)).isInstanceOf(B1.class);
    assertThat(injector.instanceOf(C.class)).isInstanceOf(C1.class);
    assertThrows(ConfigurationException.class, () -> injector.instanceOf(A.class));
  }

  @Test
  public void testNoScopeAnnotation() {
    Injector injector = new GuiceInjectorBuilder()
            .bindings(new NoScopeModule())
            .injector();

    A noScopeInstance1 = injector.instanceOf(A.class);
    A noScopeInstance2 = injector.instanceOf(A.class);
    B singletonInstance1 = injector.instanceOf(B.class);
    B singletonInstance2 = injector.instanceOf(B.class);

    assertThat(noScopeInstance1).isNotSameAs(noScopeInstance2);
    assertThat(singletonInstance1).isSameAs(singletonInstance2);
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

  public static class NoScopeModule extends com.google.inject.AbstractModule {
    @Override
    protected void configure() {
      bind(A.class).to(A1.class).in(NoScope.class);
      bind(B.class).to(B1.class).in(jakarta.inject.Singleton.class);
    }
  }

}
