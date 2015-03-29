/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.tests.guice;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;
import play.api.inject.Binding;
import play.Configuration;
import play.Environment;
import play.Mode;
import play.mvc.Result;
import scala.collection.Seq;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

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

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void setEnvironment() {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        // #set-environment
        Application application = new GuiceApplicationBuilder()
            .load(new play.api.inject.BuiltinModule(), new play.inject.BuiltInModule()) // ###skip
            .loadConfig(Configuration.reference()) // ###skip
            .in(new Environment(new File("path/to/app"), classLoader, Mode.TEST))
            .build();
        // #set-environment

        assertThat(application.path(), equalTo(new File("path/to/app")));
        assert(application.isTest());
        assertThat(application.classloader(), sameInstance(classLoader));
    }

    @Test
    public void setEnvironmentValues() {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        // #set-environment-values
        Application application = new GuiceApplicationBuilder()
            .load(new play.api.inject.BuiltinModule(), new play.inject.BuiltInModule()) // ###skip
            .loadConfig(Configuration.reference()) // ###skip
            .in(new File("path/to/app"))
            .in(Mode.TEST)
            .in(classLoader)
            .build();
        // #set-environment-values

        assertThat(application.path(), equalTo(new File("path/to/app")));
        assert(application.isTest());
        assertThat(application.classloader(), sameInstance(classLoader));
    }

    @Test
    public void addConfiguration() {
        // #add-configuration
        Configuration extraConfig = new Configuration(ImmutableMap.of("a", 1));
        Map<String, Object> configMap = ImmutableMap.of("b", 2, "c", "three");

        Application application = new GuiceApplicationBuilder()
            .configure(extraConfig)
            .configure(configMap)
            .configure("key", "value")
            .build();
        // #add-configuration

        assertThat(application.configuration().getInt("a"), equalTo(1));
        assertThat(application.configuration().getInt("b"), equalTo(2));
        assertThat(application.configuration().getString("c"), equalTo("three"));
        assertThat(application.configuration().getString("key"), equalTo("value"));
    }

    @Test
    public void overrideConfiguration() {
        // #override-configuration
        Application application = new GuiceApplicationBuilder()
            .loadConfig(env -> Configuration.load(env))
            .build();
        // #override-configuration
    }

    @Test
    public void addBindings() {
        // #add-bindings
        Application application = new GuiceApplicationBuilder()
            .bindings(new ComponentModule())
            .bindings(bind(Component.class).to(DefaultComponent.class))
            .build();
        // #add-bindings

        assertThat(application.injector().instanceOf(Component.class), instanceOf(DefaultComponent.class));
    }

    @Test
    public void overrideBindings() {
        // #override-bindings
        Application application = new GuiceApplicationBuilder()
            .configure("play.http.router", Routes.class.getName()) // ###skip
            .bindings(new ComponentModule()) // ###skip
            .overrides(bind(Component.class).to(MockComponent.class))
            .build();
        // #override-bindings

        running(application, () -> {
            Result result = route(fakeRequest(GET, "/"));
            assertThat(contentAsString(result), equalTo("mock"));
        });
    }

    @Test
    public void loadModules() {
        // #load-modules
        Application application = new GuiceApplicationBuilder()
            .load(
                Guiceable.modules(
                    new play.api.inject.BuiltinModule(),
                    new play.inject.BuiltInModule()
                ),
                Guiceable.bindings(
                    bind(Component.class).to(DefaultComponent.class)
                )
            ).build();
        // #load-modules

        assertThat(application.injector().instanceOf(Component.class), instanceOf(DefaultComponent.class));
    }

    @Test
    public void disableModules() {
        // #disable-modules
        Application application = new GuiceApplicationBuilder()
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
        Injector injector = new GuiceInjectorBuilder()
            .configure("key", "value")
            .bindings(new ComponentModule())
            .overrides(bind(Component.class).to(MockComponent.class))
            .build();

        Component component = injector.instanceOf(Component.class);
        // #injector-builder

        assertThat(component, instanceOf(MockComponent.class));
    }

}
