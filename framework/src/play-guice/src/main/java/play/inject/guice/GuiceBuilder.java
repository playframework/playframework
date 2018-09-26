/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import play.Environment;
import play.Mode;
import play.api.inject.guice.GuiceableModule;
import play.inject.Injector;
import play.libs.Scala;

import java.io.File;
import java.util.Map;

/**
 * A builder for creating Guice-backed Play Injectors.
 *
 * @param <Self> the concrete type that is extending this class
 * @param <Delegate> a scala GuiceBuilder type.
 */
public abstract class GuiceBuilder<Self, Delegate extends play.api.inject.guice.GuiceBuilder<Delegate>> {

    protected Delegate delegate;

    protected GuiceBuilder(Delegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Set the environment.
     *
     * @param env the environment to configure into this application
     * @return a copy of this builder with the new environment
     */
    public final Self in(Environment env) {
        return newBuilder(delegate.in(env.asScala()));
    }

    /**
     * Set the environment path.
     *
     * @param path the path to configure
     * @return a copy of this builder with the new path
     */
    public final Self in(File path) {
        return newBuilder(delegate.in(path));
    }

    /**
     * Set the environment mode.
     *
     * @param mode the mode to configure
     * @return a copy of this build configured with this mode
     */
    public final Self in(Mode mode) {
        return newBuilder(delegate.in(mode.asScala()));
    }

    /**
     * Set the environment class loader.
     *
     * @param classLoader the class loader to use
     * @return a copy of this builder configured with the class loader
     */
    public final Self in(ClassLoader classLoader) {
        return newBuilder(delegate.in(classLoader));
    }

    /**
     * Add additional configuration.
     *
     * @param conf the configuration to add
     * @return a copy of this builder configured with the supplied configuration
     */
    public final Self configure(Config conf) {
        return newBuilder(delegate.configure(new play.api.Configuration(conf)));
    }

    /**
     * Add additional configuration.
     *
     * @param conf the configuration to add
     * @return a copy of this builder configured with the supplied configuration
     */
    public final Self configure(Map<String, Object> conf) {
        return configure(ConfigFactory.parseMap(conf));
    }

    /**
     * Add additional configuration.
     *
     * @param key a configuration key to set
     * @param value the associated value for <code>key</code>
     * @return a copy of this builder configured with the key=value
     */
    public final Self configure(String key, Object value) {
        return configure(ImmutableMap.of(key, value));
    }

    /**
     * Add bindings from guiceable modules.
     *
     * @param modules the set of modules to bind
     * @return a copy of this builder configured with those modules
     */
    public final Self bindings(GuiceableModule... modules) {
        return newBuilder(delegate.bindings(Scala.varargs(modules)));
    }

    /**
     * Add bindings from Guice modules.
     *
     * @param modules the set of Guice modules whose bindings to apply
     * @return a copy of this builder configured with the provided bindings
     */
    public final Self bindings(Module... modules) {
        return bindings(Guiceable.modules(modules));
    }

    /**
     * Add bindings from Play modules.
     *
     * @param modules the set of Guice modules whose bindings to apply
     * @return a copy of this builder configured with the provided bindings
     */
    public final Self bindings(play.api.inject.Module... modules) {
        return bindings(Guiceable.modules(modules));
    }

    /**
     * Add Play bindings.
     *
     * @param bindings the set of play bindings to apply
     * @return a copy of this builder configured with the provided bindings
     */
    public final Self bindings(play.api.inject.Binding<?>... bindings) {
        return bindings(Guiceable.bindings(bindings));
    }

    /**
     * Override bindings using guiceable modules.
     *
     * @param modules the set of Guice modules whose bindings override some previously configured ones
     * @return a copy of this builder re-configured with the provided bindings
     */
    public final Self overrides(GuiceableModule... modules) {
        return newBuilder(delegate.overrides(Scala.varargs(modules)));
    }

    /**
     * Override bindings using Guice modules.
     *
     * @param modules the set of Guice modules whose bindings override some previously configured ones
     * @return a copy of this builder re-configured with the provided bindings
     */
    public final Self overrides(Module... modules) {
        return overrides(Guiceable.modules(modules));
    }

    /**
     * Override bindings using Play modules.
     *
     * @param modules the set of Play modules whose bindings override some previously configured ones
     * @return a copy of this builder re-configured with the provided bindings
     */
    public final Self overrides(play.api.inject.Module... modules) {
        return overrides(Guiceable.modules(modules));
    }

    /**
     * Override bindings using Play bindings.
     *
     * @param bindings a set of Play bindings that override some previously configured ones
     * @return a copy of this builder re-configured with the provided bindings
     */
    public final Self overrides(play.api.inject.Binding<?>... bindings) {
        return overrides(Guiceable.bindings(bindings));
    }

    /**
     * Disable modules by class.
     *
     * @param moduleClasses the module classes whose bindings should be disabled
     * @return a copy of this builder configured to ignore the provided module classes
     */
    public final Self disable(Class<?>... moduleClasses) {
        return newBuilder(delegate.disable(Scala.toSeq(moduleClasses)));
    }

    /**
     * Create a Guice module that can be used to inject an Application.
     *
     * @return the module
     */
    public Module applicationModule() {
        return delegate.applicationModule();
    }

    /**
     * Create a Play Injector backed by Guice using this configured builder.
     *
     * @return the injector
     */
    public Injector injector() {
        return delegate.injector().instanceOf(Injector.class);
    }

    protected abstract Self newBuilder(Delegate delegate);
}
