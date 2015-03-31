/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package detailedtopics.configuration.gzipencoding;

//#filters
import play.api.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import play.http.HttpFilters;

import javax.inject.Inject;

public class Filters implements HttpFilters {

    @Inject
    GzipFilter gzipFilter;

    public EssentialFilter[] filters() {
        return new EssentialFilter[] { gzipFilter };
    }
}
//#filters