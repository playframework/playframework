/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.configuration.gzipencoding;

//#filters
import play.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import play.http.HttpFilters;

import javax.inject.Inject;

public class Filters implements HttpFilters {

    @Inject
    GzipFilter gzipFilter;

    public EssentialFilter[] filters() {
        return new EssentialFilter[] { gzipFilter.asJava() };
    }
}
//#filters
