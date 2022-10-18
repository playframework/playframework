/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
