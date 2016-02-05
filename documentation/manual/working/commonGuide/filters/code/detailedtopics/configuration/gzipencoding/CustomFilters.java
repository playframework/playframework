/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package detailedtopics.configuration.gzipencoding;

import play.api.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import play.filters.gzip.GzipFilterConfig;
import play.http.HttpFilters;

import javax.inject.Inject;

public class CustomFilters implements HttpFilters {

    //#gzip-filter
    GzipFilter gzipFilter = new GzipFilter(
      new GzipFilterConfig().withShouldGzip((req, res) ->
        res.body().contentType().orElse("").startsWith("text/html")
      )
    );
    //#gzip-filter

    public EssentialFilter[] filters() {
        return new EssentialFilter[] { gzipFilter };
    }
}