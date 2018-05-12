/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.ConfigurationComponents;
import play.components.HttpErrorHandlerComponents;
import play.filters.cors.CORSConfig;
import play.filters.cors.CORSConfig$;
import play.filters.cors.CORSFilter;
import play.libs.Scala;

import java.util.List;

/**
 * Java Components for the CORS Filter.
 */
public interface CORSComponents extends ConfigurationComponents, HttpErrorHandlerComponents {

    default CORSConfig corsConfig() {
        return CORSConfig$.MODULE$.fromConfiguration(configuration());
    }

    default List<String> corsPathPrefixes() {
        return config().getStringList("play.filters.cors.pathPrefixes");
    }

    default CORSFilter corsFilter() {
        return new CORSFilter(
            corsConfig(),
            scalaHttpErrorHandler(),
            Scala.asScala(corsPathPrefixes())
        );
    }

}
