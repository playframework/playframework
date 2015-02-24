/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject.guice;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Map;
import play.api.inject.guice.GuiceableModule;
import play.Configuration;
import play.Environment;
import play.inject.DelegateInjector;
import play.inject.Injector;
import play.libs.Scala;
import play.Mode;

/**
 * A builder for creating Guice-backed Play Injectors.
 */
public abstract class GuiceBuilder<Self, Delegate extends play.api.inject.guice.GuiceBuilder<Delegate>> {

    protected Delegate delegate;

    protected GuiceBuilder(Delegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Set the environment.
     */
    public final Self in(Environment env) {
        return newBuilder(delegate.in(env.underlying()));
    }

    /**
     * Set the environment path.
     */
    public final Self in(File path) {
        return newBuilder(delegate.in(path));
    }

    /**
     * Set the environment mode.
     */
    public final Self in(Mode mode) {
        return newBuilder(delegate.in(play.api.Mode.apply(mode.ordinal())));
    }

    /**
     * Set the environment class loader.
     */
    public final Self in(ClassLoader classLoader) {
        return newBuilder(delegate.in(classLoader));
    }

    /**
     * Add additional configuration.
     */
    public final Self configure(Configuration conf) {
        return newBuilder(delegate.configure(conf.getWrappedConfiguration()));
    }

    /**
     * Add additional configuration.
     */
    public final Self configure(Map<String, Object> conf) {
        return configure(new Configuration(conf));
    }

    /**
     * Add additional configuration.
     */
    public final Self configure(String key, Object value) {
        return configure(ImmutableMap.of(key, value));
    }

    /**
     * Add bindings from guiceable modules.
     */
    public final Self bindings(GuiceableModule... modules) {
        return newBuilder(delegate.bindings(Scala.varargs(modules)));
    }

    /**
     * Add bindings from Guice modules.
     */
    public final Self bindings(com.google.inject.Module... modules) {
        return bindings(Guiceable.modules(modules));
    }

    /**
     * Add bindings from Play modules.
     */
    public final Self bindings(play.api.inject.Module... modules) {
        return bindings(Guiceable.modules(modules));
    }

    /**
     * Add Play bindings.
     */
    public final Self bindings(play.api.inject.Binding<?>... bindings) {
        return bindings(Guiceable.bindings(bindings));
    }

    /**
     * Override bindings using guiceable modules.
     */
    public final Self overrides(GuiceableModule... modules) {
        return newBuilder(delegate.overrides(Scala.varargs(modules)));
    }

    /**
     * Override bindings using Guice modules.
     */
    public final Self overrides(com.google.inject.Module... modules) {
        return overrides(Guiceable.modules(modules));
    }

    /**
     * Override bindings using Play modules.
     */
    public final Self overrides(play.api.inject.Module... modules) {
        return overrides(Guiceable.modules(modules));
    }

    /**
     * Override bindings using Play bindings.
     */
    public final Self overrides(play.api.inject.Binding<?>... bindings) {
        return overrides(Guiceable.bindings(bindings));
    }

    /**
     * Disable modules by class.
     */
    public final Self disable(Class<?>... moduleClasses) {
        return newBuilder(delegate.disable(Scala.toSeq(moduleClasses)));
    }

    /**
     * Create a Play Injector backed by Guice using this configured builder.
     */
    public Injector injector() {
        return delegate.injector().instanceOf(Injector.class);
    }

    protected abstract Self newBuilder(Delegate delegate);
}
