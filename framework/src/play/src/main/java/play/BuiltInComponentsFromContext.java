/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import com.typesafe.config.Config;
import play.api.inject.NewInstanceInjector$;
import play.api.inject.SimpleInjector;
import play.core.SourceMapper;
import play.inject.ApplicationLifecycle;
import play.inject.DelegateInjector;
import play.inject.Injector;
import scala.collection.immutable.Map$;

import java.util.Optional;

/**
 * Helper that provides all the built in Java components dependencies from the application loader context.
 */
public abstract class BuiltInComponentsFromContext implements BuiltInComponents {

    private final ApplicationLoader.Context context;

    public BuiltInComponentsFromContext(ApplicationLoader.Context context) {
        this.context = context;
    }

    @Override
    public Config config() {
        return context.initialConfig();
    }

    @Override
    public Environment environment() {
        return context.environment();
    }

    @Override
    public Optional<SourceMapper> sourceMapper() {
        return context.sourceMapper();
    }

    @Override
    public ApplicationLifecycle applicationLifecycle() {
        return context.applicationLifecycle();
    }

    @Override
    public Injector injector() {
        // TODO do we need to register components like the scala version?
        SimpleInjector scalaInjector = new SimpleInjector(NewInstanceInjector$.MODULE$, Map$.MODULE$.empty());
        return new DelegateInjector(scalaInjector);
    }
}