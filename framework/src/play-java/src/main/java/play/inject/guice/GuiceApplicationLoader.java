/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject.guice;

import play.api.inject.guice.GuiceableModule;
import play.libs.Scala;
import play.Application;
import play.ApplicationLoader;

/**
 * An ApplicationLoader that uses Guice to bootstrap the application.
 *
 * Subclasses can override the <code>builder</code> and <code>overrides</code>
 * methods.
 */
public class GuiceApplicationLoader implements ApplicationLoader {

    /**
     * The initial builder to start construction from.
     */
    protected final GuiceApplicationBuilder initialBuilder;

    public GuiceApplicationLoader() {
        this(new GuiceApplicationBuilder());
    }

    public GuiceApplicationLoader(GuiceApplicationBuilder initialBuilder) {
        this.initialBuilder = initialBuilder;
    }

    @Override
    public final Application load(ApplicationLoader.Context context) {
        return builder(context).build();
    }

    /**
     * Construct a builder to use for loading the given context.
     */
    public GuiceApplicationBuilder builder(ApplicationLoader.Context context) {
        return initialBuilder
            .in(context.environment())
            .loadConfig(context.initialConfiguration())
            .overrides(overrides(context));
    }

    /**
     * Override some bindings using information from the context. The default
     * implementation of this method provides bindings that most applications
     * should include.
     */
    protected GuiceableModule[] overrides(ApplicationLoader.Context context) {
        scala.collection.Seq<GuiceableModule> seq = play.api.inject.guice.GuiceApplicationLoader$.MODULE$.defaultOverrides(context.underlying());
        return Scala.asArray(GuiceableModule.class, seq);
    }

}
