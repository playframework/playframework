/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject.guice;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;
import play.api.inject.Binding;
import play.Configuration;
import play.Environment;
import play.inject.Injector;
import play.Mode;
import scala.collection.Seq;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.inject.Bindings.bind;

public class GuiceInjectorBuilderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void setEnvironment() {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        Environment env = new GuiceInjectorBuilder()
            .in(new Environment(new File("test"), classLoader, Mode.DEV))
            .bindings(new EnvironmentModule())
            .injector()
            .instanceOf(Environment.class);

        assertThat(env.rootPath(), equalTo(new File("test")));
        assertThat(env.mode(), equalTo(Mode.DEV));
        assertThat(env.classLoader(), sameInstance(classLoader));
    }

    @Test
    public void setEnvironmentValues() {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        Environment env = new GuiceInjectorBuilder()
            .in(new File("test"))
            .in(Mode.DEV)
            .in(classLoader)
            .bindings(new EnvironmentModule())
            .injector()
            .instanceOf(Environment.class);

        assertThat(env.rootPath(), equalTo(new File("test")));
        assertThat(env.mode(), equalTo(Mode.DEV));
        assertThat(env.classLoader(), sameInstance(classLoader));
    }

    @Test
    public void setConfiguration() {
        Configuration conf = new GuiceInjectorBuilder()
            .configure(new Configuration(ImmutableMap.of("a", 1)))
            .configure(ImmutableMap.of("b", 2))
            .configure("c", 3)
            .configure("d.1", 4)
            .configure("d.2", 5)
            .bindings(new ConfigurationModule())
            .injector()
            .instanceOf(Configuration.class);

        assertThat(conf.subKeys().size(), is(4));
        assertThat(conf.subKeys(), hasItems("a", "b", "c", "d"));

        assertThat(conf.getInt("a"), is(1));
        assertThat(conf.getInt("b"), is(2));
        assertThat(conf.getInt("c"), is(3));
        assertThat(conf.getInt("d.1"), is(4));
        assertThat(conf.getInt("d.2"), is(5));
    }

    @Test
    public void supportVariousBindings() {
        Injector injector = new GuiceInjectorBuilder()
            .bindings(new EnvironmentModule(), new ConfigurationModule())
            .bindings(new AModule(), new BModule())
            .bindings(bind(C.class).to(C1.class), bind(D.class).toInstance(new D1()))
            .injector();

        assertThat(injector.instanceOf(Environment.class), instanceOf(Environment.class));
        assertThat(injector.instanceOf(Configuration.class), instanceOf(Configuration.class));
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
        public Seq<Binding<?>> bindings(play.api.Environment env, play.api.Configuration conf) {
            return seq(bind(Environment.class).toInstance(new Environment(env)));
        }
    }

    public static class ConfigurationModule extends play.api.inject.Module {
        @Override
        public Seq<Binding<?>> bindings(play.api.Environment env, play.api.Configuration conf) {
            return seq(bind(Configuration.class).toInstance(new Configuration(conf)));
        }
    }

    public static interface A {}
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

    public static interface B {}
    public static class B1 implements B {}
    public static class B2 implements B {}

    public static class BModule extends com.google.inject.AbstractModule {
        public void configure() {
            bind(B.class).to(B1.class);
        }
    }

    public static interface C {}
    public static class C1 implements C {}

    public static class CModule extends com.google.inject.AbstractModule {
        public void configure() {
            bind(C.class).to(C1.class);
        }
    }

    public static interface D {}
    public static class D1 implements D {}

    public static class DModule extends com.google.inject.AbstractModule {
        public void configure() {
            bind(D.class).to(D1.class);
        }
    }

}
