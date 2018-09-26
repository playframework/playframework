/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.Environment;
import play.components.ConfigurationComponents;
import play.filters.https.RedirectHttpsConfiguration;
import play.filters.https.RedirectHttpsConfigurationProvider;
import play.filters.https.RedirectHttpsFilter;

/**
 * The Redirect to HTTPS filter components for compile time dependency injection.
 */
public interface RedirectHttpsComponents extends ConfigurationComponents {

    Environment environment();

    default RedirectHttpsConfiguration redirectHttpsConfiguration() {
        return new RedirectHttpsConfigurationProvider(configuration(), environment().asScala()).get();
    }

    default RedirectHttpsFilter redirectHttpsFilter() {
        return new RedirectHttpsFilter(redirectHttpsConfiguration());
    }
}
