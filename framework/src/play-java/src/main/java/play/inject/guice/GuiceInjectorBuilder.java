/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.inject.guice;

import play.inject.Injector;

/**
 * Default empty builder for creating Guice-backed Injectors.
 */
public final class GuiceInjectorBuilder extends GuiceBuilder<GuiceInjectorBuilder, play.api.inject.guice.GuiceInjectorBuilder> {

    public GuiceInjectorBuilder() {
        this(new play.api.inject.guice.GuiceInjectorBuilder());
    }

    private GuiceInjectorBuilder(play.api.inject.guice.GuiceInjectorBuilder builder) {
        super(builder);
    }

    protected GuiceInjectorBuilder newBuilder(play.api.inject.guice.GuiceInjectorBuilder builder) {
        return new GuiceInjectorBuilder(builder);
    }

    /**
     * Create a Play Injector backed by Guice using this configured builder.
     */
    public Injector build() {
        return injector();
    }

}
