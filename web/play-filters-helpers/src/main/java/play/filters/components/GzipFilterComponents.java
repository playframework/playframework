/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.AkkaComponents;
import play.components.ConfigurationComponents;
import play.filters.gzip.GzipFilter;
import play.filters.gzip.GzipFilterConfig;
import play.filters.gzip.GzipFilterConfig$;

/** The GZIP filter Java components. */
public interface GzipFilterComponents extends ConfigurationComponents, AkkaComponents {

  default GzipFilterConfig gzipFilterConfig() {
    return GzipFilterConfig$.MODULE$.fromConfiguration(configuration());
  }

  default GzipFilter gzipFilter() {
    return new GzipFilter(gzipFilterConfig(), materializer());
  }
}
