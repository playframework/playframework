/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject.guice;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Provider;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;
import play.api.inject.guice.GuiceApplicationBuilderSpec;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.inject.Injector;
import play.libs.Scala;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.inject.Bindings.bind;

public class GuiceApplicationBuilderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void addBindings() {
        Application app = new GuiceApplicationBuilder()
            .bindings(new AModule())
            .bindings(bind(B.class).to(B1.class))
            .build();

        assertThat(app.injector().instanceOf(A.class), instanceOf(A1.class));
        assertThat(app.injector().instanceOf(B.class), instanceOf(B1.class));
    }

    @Test
    public void overrideBindings() {
        Application app = new GuiceApplicationBuilder()
            .bindings(new AModule())
            .overrides(
                // override the scala api configuration, which should underlie the java api configuration
                bind(play.api.Configuration.class).to(new GuiceApplicationBuilderSpec.ExtendConfiguration(Scala.varargs(Scala.Tuple("a", 1)))),
                // also override the java api configuration
                bind(Configuration.class).to(new ExtendConfiguration(new Configuration(ImmutableMap.of("b", 2)))),
                bind(A.class).to(A2.class))
            .build();

        assertThat(app.configuration().getInt("a"), is(1));
        assertThat(app.configuration().getInt("b"), is(2));
        assertThat(app.injector().instanceOf(A.class), instanceOf(A2.class));
    }

    @Test
    public void disableModules() {
        Application app = new GuiceApplicationBuilder()
            .bindings(new AModule())
            .disable(AModule.class)
            .build();

        exception.expect(com.google.inject.ConfigurationException.class);
        app.injector().instanceOf(A.class);
    }

    @Test
    public void disableLoadedModules() {
        Application app = new GuiceApplicationBuilder()
            .disable(play.api.libs.concurrent.AkkaModule.class)
            .build();

        exception.expect(com.google.inject.ConfigurationException.class);
        app.injector().instanceOf(akka.actor.ActorSystem.class);
    }

    @Test
    public void setInitialConfigurationLoader() {
        Configuration extra = new Configuration(ImmutableMap.of("a", 1));
        Application app = new GuiceApplicationBuilder()
            .loadConfig(env -> extra.withFallback(Configuration.load(env)))
            .build();

        assertThat(app.configuration().getInt("a"), is(1));
    }

    @Test
    public void setGlobal() {
        GlobalSettings global = new GlobalSettings() {
            @Override
            public Configuration onLoadConfig(Configuration config, File path, ClassLoader classloader) {
                Configuration extra = new Configuration(ImmutableMap.of("a", 1));
                return extra.withFallback(config);
            }
        };

        Application app = new GuiceApplicationBuilder()
            .global(global)
            .build();

        assertThat(app.configuration().getInt("a"), is(1));
    }

    @Test
    public void setModuleLoader() {
        Application app = new GuiceApplicationBuilder()
            .load((env, conf) -> ImmutableList.of(
                Guiceable.modules(new play.api.inject.BuiltinModule(), new play.inject.BuiltInModule()),
                Guiceable.bindings(bind(A.class).to(A1.class))))
            .build();

        assertThat(app.injector().instanceOf(A.class), instanceOf(A1.class));
    }

    @Test
    public void setLoadedModulesDirectly() {
        Application app = new GuiceApplicationBuilder()
            .load(
                Guiceable.modules(new play.api.inject.BuiltinModule(), new play.inject.BuiltInModule()),
                Guiceable.bindings(bind(A.class).to(A1.class)))
            .build();

        assertThat(app.injector().instanceOf(A.class), instanceOf(A1.class));
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

    public static class ExtendConfiguration implements Provider<Configuration> {

      @Inject Injector injector = null;

      Configuration extra;

      public ExtendConfiguration(Configuration extra) {
        this.extra = extra;
      }

      public Configuration get() {
        Configuration current = injector.instanceOf(play.inject.ConfigurationProvider.class).get();
        return extra.withFallback(current);
      }
    }

}
