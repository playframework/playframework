/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import java.util.List;
import play.components.ConfigurationComponents;
import play.components.HttpErrorHandlerComponents;
import play.filters.cors.CORSConfig;
import play.filters.cors.CORSConfig$;
import play.filters.cors.CORSFilter;
import play.libs.Scala;

/** Java Components for the CORS Filter. */
public interface CORSComponents extends ConfigurationComponents, HttpErrorHandlerComponents {

  default CORSConfig corsConfig() {
    return CORSConfig$.MODULE$.fromConfiguration(configuration());
  }

  default List<String> corsPathPrefixes() {
    return config().getStringList("play.filters.cors.pathPrefixes");
  }

  default CORSFilter corsFilter() {
    return new CORSFilter(corsConfig(), scalaHttpErrorHandler(), Scala.asScala(corsPathPrefixes()));
  }
}
