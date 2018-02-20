/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import play.Environment;
import play.core.SourceMapper;
import play.inject.ApplicationLifecycle;
import play.inject.Injector;
import play.routing.Router;

import java.util.Optional;

public interface BaseComponents extends ConfigurationComponents {

    /**
     * The application environment.
     * @return an instance of the application environment
     */
    Environment environment();

    Optional<SourceMapper> sourceMapper();

    ApplicationLifecycle applicationLifecycle();

    Router router();
}
