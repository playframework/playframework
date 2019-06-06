/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.configuration.gzipencoding;

import akka.stream.Materializer;
import play.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import play.filters.gzip.GzipFilterConfig;
import play.http.HttpFilters;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

public class CustomFilters implements HttpFilters {

  private List<EssentialFilter> filters;

  @Inject
  public CustomFilters(Materializer materializer) {
    // #gzip-filter
    GzipFilterConfig gzipFilterConfig = new GzipFilterConfig();
    GzipFilter gzipFilter =
        new GzipFilter(
            gzipFilterConfig.withShouldGzip(
                (BiFunction<Http.RequestHeader, Result, Object>)
                    (req, res) -> res.body().contentType().orElse("").startsWith("text/html")),
            materializer);
    // #gzip-filter
    filters = Collections.singletonList(gzipFilter.asJava());
  }

  @Override
  public List<EssentialFilter> getFilters() {
    return filters;
  }
}
