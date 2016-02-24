/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.inject.guice;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.List;
import play.api.inject.guice.GuiceableModule;
import play.Application;
import play.Configuration;
import play.core.j.JavaGlobalSettingsAdapter;
import play.Environment;
import play.GlobalSettings;
import play.libs.Scala;

import static scala.compat.java8.JFunction.func;

public final class GuiceApplicationBuilder extends GuiceBuilder<GuiceApplicationBuilder, play.api.inject.guice.GuiceApplicationBuilder> {

    public GuiceApplicationBuilder() {
        this(new play.api.inject.guice.GuiceApplicationBuilder());
    }

    private GuiceApplicationBuilder(play.api.inject.guice.GuiceApplicationBuilder builder) {
        super(builder);
    }

    public static GuiceApplicationBuilder fromScalaBuilder(play.api.inject.guice.GuiceApplicationBuilder builder) {
        return new GuiceApplicationBuilder(builder);
    }

    /**
     * Set the initial configuration loader.
     * Overrides the default or any previously configured values.
     *
     * @param load the configuration loader
     * @return the configured application builder
     */
    public GuiceApplicationBuilder loadConfig(Function<Environment, Configuration> load) {
        return newBuilder(delegate.loadConfig(func((play.api.Environment env) -> load.apply(new Environment(env)).getWrappedConfiguration())));
    }

    /**
     * Set the initial configuration.
     * Overrides the default or any previously configured values.
     *
     * @param conf the configuration
     * @return the configured application builder
     */
    public GuiceApplicationBuilder loadConfig(Configuration conf) {
        return loadConfig(env -> conf);
    }

    /**
     * Set the global settings object.
     * Overrides the default or any previously configured values.
     *
     * @deprecated use dependency injection, since 2.5.0
     * @param global the configuration
     * @return the configured application builder
     */
    @Deprecated
    public GuiceApplicationBuilder global(GlobalSettings global) {
        return newBuilder(delegate.global(new JavaGlobalSettingsAdapter(global)));
    }

    /**
     * Set the module loader.
     * Overrides the default or any previously configured values.
     *
     * @param loader the configuration
     * @return the configured application builder
     */
    public GuiceApplicationBuilder load(BiFunction<Environment, Configuration, List<GuiceableModule>> loader) {
        return newBuilder(delegate.load(func((play.api.Environment env, play.api.Configuration conf) ->
            Scala.toSeq(loader.apply(new Environment(env), new Configuration(conf)))
        )));
    }

    /**
     * Override the module loader with the given guiceable modules.
     *
     * @param modules the set of overriding modules
     * @return an application builder that incorporates the overrides
     */
    public GuiceApplicationBuilder load(GuiceableModule... modules) {
        return newBuilder(delegate.load(Scala.varargs(modules)));
    }

    /**
     * Override the module loader with the given Guice modules.
     *
     * @param modules the set of overriding modules
     * @return an application builder that incorporates the overrides
     */
    public GuiceApplicationBuilder load(com.google.inject.Module... modules) {
        return load(Guiceable.modules(modules));
    }

    /**
     * Override the module loader with the given Play modules.
     *
     * @param modules the set of overriding modules
     * @return an application builder that incorporates the overrides
     */
    public GuiceApplicationBuilder load(play.api.inject.Module... modules) {
        return load(Guiceable.modules(modules));
    }

    /**
     * Override the module loader with the given Play bindings.
     *
     * @param bindings the set of binding override
     * @return an application builder that incorporates the overrides
     */
    public GuiceApplicationBuilder load(play.api.inject.Binding<?>... bindings) {
        return load(Guiceable.bindings(bindings));
    }

    /**
     * Create a new Play Application using this configured builder.
     *
     * @return the application
     */
    public Application build() {
        return injector().instanceOf(Application.class);
    }

    /**
     * Implementation of Self creation for GuiceBuilder.
     *
     * @return the application builder
     */
    protected GuiceApplicationBuilder newBuilder(play.api.inject.guice.GuiceApplicationBuilder builder) {
        return new GuiceApplicationBuilder(builder);
    }

}
