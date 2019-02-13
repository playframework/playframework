/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.ConfigurationComponents;
import play.components.HttpErrorHandlerComponents;
import play.filters.hosts.AllowedHostsConfig;
import play.filters.hosts.AllowedHostsFilter;

/**
 * Java Components for the Allowed Hosts filter.
 *
 * @see AllowedHostsFilter
 */
public interface AllowedHostsComponents extends ConfigurationComponents, HttpErrorHandlerComponents {

    default AllowedHostsConfig allowedHostsConfig() {
        return AllowedHostsConfig.fromConfiguration(configuration());
    }

    default AllowedHostsFilter allowedHostsFilter() {
        return new AllowedHostsFilter(allowedHostsConfig(), scalaHttpErrorHandler());
    }

}
