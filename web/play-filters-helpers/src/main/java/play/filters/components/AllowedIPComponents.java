/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.Environment;
import play.components.ConfigurationComponents;
import play.filters.ip.AllowedIPConfiguration;
import play.filters.ip.AllowedIPConfigurationProvider;
import play.filters.ip.AllowedIPFilter;

/** The Allowed IP filter components for compile time dependency injection. */
public interface AllowedIPComponents extends ConfigurationComponents {

  Environment environment();

  default AllowedIPConfiguration allowedIPConfiguration() {
    return new AllowedIPConfigurationProvider(configuration(), environment().asScala()).get();
  }

  default AllowedIPFilter allowedIPFilter() {
    return new AllowedIPFilter(allowedIPConfiguration());
  }
}
