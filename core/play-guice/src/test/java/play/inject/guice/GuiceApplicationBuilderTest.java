/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import static org.junit.jupiter.api.Assertions.*;
import static play.inject.Bindings.bind;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.ConfigurationException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javax.inject.Inject;
import javax.inject.Provider;
import org.junit.jupiter.api.Test;
import play.Application;
import play.api.inject.guice.GuiceApplicationBuilderSpec;
import play.inject.Injector;
import play.libs.Scala;

class GuiceApplicationBuilderTest {

  @Test
  void addBindings() {
    Injector injector =
        new GuiceApplicationBuilder()
            .bindings(new AModule())
            .bindings(bind(B.class).to(B1.class))
            .injector();

    assertInstanceOf(A1.class, injector.instanceOf(A.class));
    assertInstanceOf(B1.class, injector.instanceOf(B.class));
  }

  @Test
  void overrideBindings() {
    Application app =
        new GuiceApplicationBuilder()
            .bindings(new AModule())
            .overrides(
                // override the scala api configuration, which should underlie the java api
                // configuration
                bind(play.api.Configuration.class)
                    .to(
                        new GuiceApplicationBuilderSpec.ExtendConfiguration(
                            Scala.varargs(Scala.Tuple("a", 1)))),
                // also override the java api configuration
                bind(Config.class)
                    .to(new ExtendConfiguration(ConfigFactory.parseMap(ImmutableMap.of("b", 2)))),
                bind(A.class).to(A2.class))
            .injector()
            .instanceOf(Application.class);

    assertEquals(1, app.config().getInt("a"));
    assertEquals(2, app.config().getInt("b"));
    assertInstanceOf(A2.class, app.injector().instanceOf(A.class));
  }

  @Test
  void disableModules() {
    Injector injector =
        new GuiceApplicationBuilder().bindings(new AModule()).disable(AModule.class).injector();
    assertThrowsExactly(ConfigurationException.class, () -> injector.instanceOf(A.class));
  }

  @Test
  void setInitialConfigurationLoader() {
    Config extra = ConfigFactory.parseMap(ImmutableMap.of("a", 1));
    Application app =
        new GuiceApplicationBuilder()
            .withConfigLoader(env -> extra.withFallback(ConfigFactory.load(env.classLoader())))
            .build();

    assertEquals(1, app.config().getInt("a"));
  }

  @Test
  void setModuleLoader() {
    Injector injector =
        new GuiceApplicationBuilder()
            .withModuleLoader(
                (env, conf) ->
                    ImmutableList.of(
                        Guiceable.modules(
                            new play.api.inject.BuiltinModule(),
                            new play.api.i18n.I18nModule(),
                            new play.api.mvc.CookiesModule()),
                        Guiceable.bindings(bind(A.class).to(A1.class))))
            .injector();

    assertInstanceOf(A1.class, injector.instanceOf(A.class));
  }

  @Test
  void setLoadedModulesDirectly() {
    Injector injector =
        new GuiceApplicationBuilder()
            .load(
                Guiceable.modules(
                    new play.api.inject.BuiltinModule(),
                    new play.api.i18n.I18nModule(),
                    new play.api.mvc.CookiesModule()),
                Guiceable.bindings(bind(A.class).to(A1.class)))
            .injector();

    assertInstanceOf(A1.class, injector.instanceOf(A.class));
  }

  public static interface A {}

  public static class A1 implements A {}

  public static class A2 implements A {}

  public static class AModule extends com.google.inject.AbstractModule {
    public void configure() {
      bind(A.class).to(A1.class);
    }
  }

  public static interface B {}

  public static class B1 implements B {}

  public static class ExtendConfiguration implements Provider<Config> {

    @Inject Injector injector = null;

    Config extra;

    public ExtendConfiguration(Config extra) {
      this.extra = extra;
    }

    public Config get() {
      Config current = injector.instanceOf(play.api.inject.ConfigProvider.class).get();
      return extra.withFallback(current);
    }
  }
}
