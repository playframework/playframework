/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;
import play.Environment;
import play.inject.Binding;
import play.inject.Injector;
import play.inject.Module;
import play.Mode;
import scala.collection.Seq;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.inject.Bindings.bind;

public class GuiceInjectorBuilderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

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
        Environment env = new GuiceInjectorBuilder()
            .in(new Environment(new File("test"), classLoader, Mode.DEV))
            .bindings(environmentModule)
            .injector()
            .instanceOf(Environment.class);

        assertThat(env.rootPath(), equalTo(new File("test")));
        assertThat(env.mode(), equalTo(Mode.DEV));
        assertThat(env.classLoader(), sameInstance(classLoader));
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
        Environment env = new GuiceInjectorBuilder()
            .in(new File("test"))
            .in(Mode.DEV)
            .in(classLoader)
            .bindings(environmentModule)
            .injector()
            .instanceOf(Environment.class);

        assertThat(env.rootPath(), equalTo(new File("test")));
        assertThat(env.mode(), equalTo(Mode.DEV));
        assertThat(env.classLoader(), sameInstance(classLoader));
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
        Config conf = new GuiceInjectorBuilder()
            .configure(ConfigFactory.parseMap(ImmutableMap.of("a", 1)))
            .configure(ImmutableMap.of("b", 2))
            .configure("c", 3)
            .configure("d.1", 4)
            .configure("d.2", 5)
            .bindings(configurationModule)
            .injector()
            .instanceOf(Config.class);

        assertThat(conf.root().keySet().size(), is(4));
        assertThat(conf.root().keySet(), org.junit.matchers.JUnitMatchers.hasItems("a", "b", "c", "d"));

        assertThat(conf.getInt("a"), is(1));
        assertThat(conf.getInt("b"), is(2));
        assertThat(conf.getInt("c"), is(3));
        assertThat(conf.getInt("d.1"), is(4));
        assertThat(conf.getInt("d.2"), is(5));
    }

    @Test
    public void supportVariousBindingsWithScala() {
        supportVariousBindings(new EnvironmentModule(), new ConfigurationModule());
    }

    @Test
    public void supportVariousBindingsWithJava() {
        supportVariousBindings(new JavaEnvironmentModule(), new JavaConfigurationModule());
    }

    private void supportVariousBindings(play.api.inject.Module environmentModule, play.api.inject.Module configurationModule) {
        Injector injector = new GuiceInjectorBuilder()
            .bindings(environmentModule, configurationModule)
            .bindings(new AModule(), new BModule())
            .bindings(bind(C.class).to(C1.class), bind(D.class).toInstance(new D1()))
            .injector();

        assertThat(injector.instanceOf(Environment.class), instanceOf(Environment.class));
        assertThat(injector.instanceOf(Config.class), instanceOf(Config.class));
        assertThat(injector.instanceOf(A.class), instanceOf(A1.class));
        assertThat(injector.instanceOf(B.class), instanceOf(B1.class));
        assertThat(injector.instanceOf(C.class), instanceOf(C1.class));
        assertThat(injector.instanceOf(D.class), instanceOf(D1.class));
    }

    @Test
    public void overrideBindings() {
        Injector injector = new GuiceInjectorBuilder()
            .bindings(new AModule())
            .bindings(bind(B.class).to(B1.class))
            .overrides(new A2Module())
            .overrides(bind(B.class).to(B2.class))
            .injector();

        assertThat(injector.instanceOf(A.class), instanceOf(A2.class));
        assertThat(injector.instanceOf(B.class), instanceOf(B2.class));
    }

    @Test
    public void disableModules() {
        Injector injector = new GuiceInjectorBuilder()
            .bindings(new AModule(), new BModule())
            .bindings(bind(C.class).to(C1.class))
            .disable(AModule.class, CModule.class) // C won't be disabled
            .injector();

        assertThat(injector.instanceOf(B.class), instanceOf(B1.class));
        assertThat(injector.instanceOf(C.class), instanceOf(C1.class));

        exception.expect(com.google.inject.ConfigurationException.class);
        injector.instanceOf(A.class);
    }

    public static class EnvironmentModule extends play.api.inject.Module {
        @Override
        public Seq<play.api.inject.Binding<?>> bindings(play.api.Environment env, play.api.Configuration conf) {
            return seq(bind(Environment.class).toInstance(new Environment(env)));
        }
    }

    public static class ConfigurationModule extends play.api.inject.Module {
        @Override
        public Seq<play.api.inject.Binding<?>> bindings(play.api.Environment env, play.api.Configuration conf) {
            return seq(bind(Config.class).toInstance(conf.underlying()));
        }
    }

    public static class JavaEnvironmentModule extends Module {
        @Override
        public List<Binding<?>> bindings(Environment env, Config conf) {
            return Collections.singletonList(bindClass(Environment.class).toInstance(new Environment(env.asScala())));
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
