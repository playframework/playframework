/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.ConfigurationComponents;
import play.filters.headers.SecurityHeadersConfig;
import play.filters.headers.SecurityHeadersFilter;

/**
 * The security headers Java components.
 *
 * @see SecurityHeadersFilter
 */
public interface SecurityHeadersComponents extends ConfigurationComponents {

    default SecurityHeadersConfig securityHeadersConfig() {
        return SecurityHeadersConfig.fromConfiguration(configuration());
    }

    default SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter(securityHeadersConfig());
    }
}
