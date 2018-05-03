/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.AkkaComponents;
import play.components.ConfigurationComponents;
import play.filters.gzip.GzipFilter;
import play.filters.gzip.GzipFilterConfig;
import play.filters.gzip.GzipFilterConfig$;

/**
 * The GZIP filter Java components.
 */
public interface GzipFilterComponents extends ConfigurationComponents, AkkaComponents {

    default GzipFilterConfig gzipFilterConfig() {
        return GzipFilterConfig$.MODULE$.fromConfiguration(configuration());
    }

    default GzipFilter gzipFilter() {
        return new GzipFilter(gzipFilterConfig(), materializer());
    }
}
