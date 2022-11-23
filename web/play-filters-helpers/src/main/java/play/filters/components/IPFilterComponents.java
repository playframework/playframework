/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.Environment;
import play.components.ConfigurationComponents;
import play.filters.ip.IPFilterConfiguration;
import play.filters.ip.IPFilterConfigurationProvider;
import play.filters.ip.IPFilter;

/** The Allowed IP filter components for compile time dependency injection. */
public interface IPFilterComponents extends ConfigurationComponents {

  Environment environment();

  default IPFilterConfiguration ipFilterConfiguration() {
    return new IPFilterConfigurationProvider(configuration(), environment().asScala()).get();
  }

  default IPFilter ipFilter() {
    return new IPFilter(ipFilterConfiguration());
  }
}
