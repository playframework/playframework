/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.Environment;
import play.components.ConfigurationComponents;
import play.components.HttpErrorHandlerComponents;
import play.filters.ip.IPFilter;
import play.filters.ip.IPFilterConfig;
import play.filters.ip.IPFilterConfigProvider;

/** The IP filter components for compile time dependency injection. */
public interface IPFilterComponents extends ConfigurationComponents, HttpErrorHandlerComponents {

  Environment environment();

  default IPFilterConfig ipFilterConfig() {
    return new IPFilterConfigProvider(configuration()).get();
  }

  default IPFilter ipFilter() {
    return new IPFilter(ipFilterConfig(), scalaHttpErrorHandler());
  }
}
