/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.Environment;
import play.components.ConfigurationComponents;
import play.filters.ip.IPFilterConfig;
import play.filters.ip.IPFilterConfigProvider;
import play.filters.ip.IPFilter;

/** The IP filter components for compile time dependency injection. */
public interface IPFilterComponents extends ConfigurationComponents {

  Environment environment();

  default IPFilterConfig ipFilterConfig() {
    return new IPFilterConfigProvider(configuration()).get();
  }

  default IPFilter ipFilter() {
    return new IPFilter(ipFilterConfig());
  }
}
