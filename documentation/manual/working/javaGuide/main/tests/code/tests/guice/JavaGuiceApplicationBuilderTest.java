/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests.guice;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;
import play.Environment;
import play.Mode;
import play.api.Configuration;
import play.mvc.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.*;

import static javaguide.tests.FakeApplicationTest.Computer;

// #builder-imports
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
// #builder-imports

// #bind-imports
import static play.inject.Bindings.bind;
// #bind-imports

// #guiceable-imports
import play.inject.guice.Guiceable;
// #guiceable-imports

// #injector-imports
import play.inject.Injector;
import play.inject.guice.GuiceInjectorBuilder;
// #injector-imports

public class JavaGuiceApplicationBuilderTest {

  @Rule public ExpectedException exception = ExpectedException.none();

  @Test
  public void setEnvironment() {
    ClassLoader classLoader = new URLClassLoader(new URL[0]);
    // #set-environment
    Application application =
        new GuiceApplicationBuilder()
            .load(
                new play.api.inject.BuiltinModule(),
                new play.inject.BuiltInModule(),
                new play.api.i18n.I18nModule(),
                new play.api.mvc.CookiesModule()) // ###skip
            .loadConfig(ConfigFactory.defaultReference()) // ###skip
            .configure("play.http.filters", "play.api.http.NoHttpFilters") // ###skip
            .in(new Environment(new File("path/to/app"), classLoader, Mode.TEST))
            .build();
    // #set-environment

    assertThat(application.path()).isEqualTo(new File("path/to/app"));
    assert (application.isTest());
    assertThat(application.classloader()).isSameAs(classLoader);
  }

  @Test
  public void setEnvironmentValues() {
    ClassLoader classLoader = new URLClassLoader(new URL[0]);
    // #set-environment-values
    Application application =
        new GuiceApplicationBuilder()
            .load(
                new play.api.inject.BuiltinModule(),
                new play.inject.BuiltInModule(),
                new play.api.i18n.I18nModule(),
                new play.api.mvc.CookiesModule()) // ###skip
            .loadConfig(ConfigFactory.defaultReference()) // ###skip
            .configure("play.http.filters", "play.api.http.NoHttpFilters") // ###skip
            .in(new File("path/to/app"))
            .in(Mode.TEST)
            .in(classLoader)
            .build();
    // #set-environment-values

    assertThat(application.path()).isEqualTo(new File("path/to/app"));
    assert (application.isTest());
    assertThat(application.classloader()).isSameAs(classLoader);
  }

  @Test
  public void addConfiguration() {
    // #add-configuration
    Config extraConfig = ConfigFactory.parseMap(ImmutableMap.of("a", 1));
    Map<String, Object> configMap = ImmutableMap.of("b", 2, "c", "three");

    Application application =
        new GuiceApplicationBuilder()
            .configure(extraConfig)
            .configure(configMap)
            .configure("key", "value")
            .build();
    // #add-configuration

    assertThat(application.config().getInt("a")).isEqualTo(1);
    assertThat(application.config().getInt("b")).isEqualTo(2);
    assertThat(application.config().getString("c")).isEqualTo("three");
    assertThat(application.config().getString("key")).isEqualTo("value");
  }

  @Test
  public void overrideConfiguration() {
    // #override-configuration
    Application application =
        new GuiceApplicationBuilder()
            .withConfigLoader(env -> ConfigFactory.load(env.classLoader()))
            .build();
    // #override-configuration
  }

  @Test
  public void addBindings() {
    // #add-bindings
    Application application =
        new GuiceApplicationBuilder()
            .bindings(new ComponentModule())
            .bindings(bind(Component.class).to(DefaultComponent.class))
            .build();
    // #add-bindings

    assertThat(application.injector().instanceOf(Component.class))
        .isInstanceOf(DefaultComponent.class);
  }

  @Test
  public void overrideBindings() {
    // #override-bindings
    Application application =
        new GuiceApplicationBuilder()
            .configure("play.http.router", Routes.class.getName()) // ###skip
            .configure("play.http.filters", "play.api.http.NoHttpFilters") // ###skip
            .bindings(new ComponentModule()) // ###skip
            .overrides(bind(Component.class).to(MockComponent.class))
            .build();
    // #override-bindings

    running(
        application,
        () -> {
          Result result = route(application, fakeRequest(GET, "/"));
          assertThat(contentAsString(result)).isEqualTo("mock");
        });
  }

  @Test
  public void loadModules() {
    // #load-modules
    Application application =
        new GuiceApplicationBuilder()
            .configure("play.http.filters", "play.api.http.NoHttpFilters") // ###skip
            .load(
                Guiceable.modules(
                    new play.api.inject.BuiltinModule(),
                    new play.api.i18n.I18nModule(),
                    new play.api.mvc.CookiesModule(),
                    new play.inject.BuiltInModule()),
                Guiceable.bindings(bind(Component.class).to(DefaultComponent.class)))
            .build();
    // #load-modules

    assertThat(application.injector().instanceOf(Component.class))
        .isInstanceOf(DefaultComponent.class);
  }

  @Test
  public void disableModules() {
    // #disable-modules
    Application application =
        new GuiceApplicationBuilder()
            .configure("play.http.filters", "play.api.http.NoHttpFilters") // ###skip
            .bindings(new ComponentModule()) // ###skip
            .disable(ComponentModule.class)
            .build();
    // #disable-modules

    exception.expect(com.google.inject.ConfigurationException.class);
    application.injector().instanceOf(Component.class);
  }

  @Test
  public void injectorBuilder() {
    // #injector-builder
    Injector injector =
        new GuiceInjectorBuilder()
            .configure("key", "value")
            .bindings(new ComponentModule())
            .overrides(bind(Component.class).to(MockComponent.class))
            .injector();

    Component component = injector.instanceOf(Component.class);
    // #injector-builder

    assertThat(component).isInstanceOf(MockComponent.class);
  }

  // #test-guiceapp
  @Test
  public void findById() {
    ClassLoader classLoader = classLoader();
    Application application =
        new GuiceApplicationBuilder()
            .in(new Environment(new File("path/to/app"), classLoader, Mode.TEST))
            .build();

    running(
        application,
        () -> {
          Computer macintosh = Computer.findById(21l);
          assertThat(macintosh.name).isEqualTo("Macintosh");
          assertThat(macintosh.introduced).isEqualTo("1984-01-24");
        });
  }
  // #test-guiceapp

  private ClassLoader classLoader() {
    return new URLClassLoader(new URL[0]);
  }
}
