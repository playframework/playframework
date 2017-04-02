/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.components;

import play.components.ConfigurationComponents;
import play.components.HttpErrorHandlerComponents;
import play.components.JavaContextComponentsComponents;
import play.core.j.JavaHttpErrorHandlerAdapter;
import play.filters.hosts.AllowedHostsConfig;
import play.filters.hosts.AllowedHostsFilter;

/**
 * Java Components for the Allowed Hosts filter.
 *
 * @see AllowedHostsFilter
 */
public interface AllowedHostsComponents extends ConfigurationComponents,
        HttpErrorHandlerComponents,
        JavaContextComponentsComponents {

    default AllowedHostsConfig allowedHostsConfig() {
        return AllowedHostsConfig.fromConfiguration(configuration());
    }

    default AllowedHostsFilter allowedHostsFilter() {
        return new AllowedHostsFilter(
                allowedHostsConfig(),
                new JavaHttpErrorHandlerAdapter(
                        httpErrorHandler(),
                        javaContextComponents()
                )
        );
    }

}
