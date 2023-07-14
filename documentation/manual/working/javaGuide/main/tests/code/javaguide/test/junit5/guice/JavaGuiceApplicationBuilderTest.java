/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5.guice;

import static javaguide.test.junit5.FakeApplicationTest.Computer;
import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import org.junit.jupiter.api.Test;

import play.Environment;
import play.Mode;
import play.mvc.Result;
import router.Routes;


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

class JavaGuiceApplicationBuilderTest {

  @Test
  void setEnvironment() {
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

    assertEquals(application.path(), new File("path/to/app"));
    assertTrue(application.isTest());
    assertSame(application.classloader(), classLoader);
  }

  @Test
  void setEnvironmentValues() {
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

    assertEquals(application.path(), new File("path/to/app"));
    assertTrue(application.isTest());
    assertSame(application.classloader(), classLoader);
  }

  @Test
  void addConfiguration() {
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

    assertEquals(1, application.config().getInt("a"));
    assertEquals(2, application.config().getInt("b"));
    assertEquals("three", application.config().getString("c"));
    assertEquals("value", application.config().getString("key"));
  }

  @Test
  void overrideConfiguration() {
    // #override-configuration
    Application application =
        new GuiceApplicationBuilder()
            .withConfigLoader(env -> ConfigFactory.load(env.classLoader()))
            .build();

    assertNotNull(application);
    // #override-configuration
  }

  @Test
  void addBindings() {
    // #add-bindings
    Application application =
        new GuiceApplicationBuilder()
            .bindings(new ComponentModule())
            .bindings(bind(Component.class).to(DefaultComponent.class))
            .build();
    // #add-bindings

    Component component = application.injector().instanceOf(Component.class);
    assertInstanceOf(DefaultComponent.class, component);
  }

  @Test
  void overrideBindings() {
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
          assertEquals("mock", contentAsString(result));
        });
  }

  @Test
  void loadModules() {
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

    Component component = application.injector().instanceOf(Component.class);
    assertInstanceOf(DefaultComponent.class, component);
  }

  @Test
  void disableModules() {
    // #disable-modules
    Application application =
        new GuiceApplicationBuilder()
            .configure("play.http.filters", "play.api.http.NoHttpFilters") // ###skip
            .bindings(new ComponentModule()) // ###skip
            .disable(ComponentModule.class)
            .build();
    // #disable-modules

    assertThrowsExactly(
        com.google.inject.ConfigurationException.class,
        () -> application.injector().instanceOf(Component.class));
  }

  @Test
  void injectorBuilder() {
    // #injector-builder
    Injector injector =
        new GuiceInjectorBuilder()
            .configure("key", "value")
            .bindings(new ComponentModule())
            .overrides(bind(Component.class).to(MockComponent.class))
            .injector();

    Component component = injector.instanceOf(Component.class);
    // #injector-builder

    assertInstanceOf(MockComponent.class, component);
  }

  // #test-guiceapp
  @Test
  void findById() {
    ClassLoader classLoader = classLoader();
    Application application =
        new GuiceApplicationBuilder()
            .in(new Environment(new File("path/to/app"), classLoader, Mode.TEST))
            .build();

    running(
        application,
        () -> {
          Computer macintosh = Computer.findById(21L);
          assertEquals("Macintosh", macintosh.name);
          assertEquals("1984-01-24", macintosh.introduced);
        });
  }
  // #test-guiceapp

  private ClassLoader classLoader() {
    return new URLClassLoader(new URL[0]);
  }
}
