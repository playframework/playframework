/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.typesafe.config.Config;
import play.Application;
import play.Environment;
import play.api.inject.guice.GuiceableModule;
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
    public GuiceApplicationBuilder withConfigLoader(Function<Environment, Config> load) {
        return newBuilder(delegate.loadConfig(func((play.api.Environment env) ->
            new play.api.Configuration(load.apply(new Environment(env))))));
    }

    /**
     * Set the initial configuration.
     * Overrides the default or any previously configured values.
     *
     * @param conf the configuration
     * @return the configured application builder
     */
    public GuiceApplicationBuilder loadConfig(Config conf) {
        return withConfigLoader(env -> conf);
    }

    /**
     * Set the module loader.
     * Overrides the default or any previously configured values.
     *
     * @param loader the configuration
     * @return the configured application builder
     */
    public GuiceApplicationBuilder withModuleLoader(BiFunction<Environment, Config, List<GuiceableModule>> loader) {
        return newBuilder(delegate.load(func((play.api.Environment env, play.api.Configuration conf) ->
            Scala.toSeq(loader.apply(new Environment(env), conf.underlying()))
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
