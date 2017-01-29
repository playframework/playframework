/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.configuration.headers;

//#filters
import play.mvc.EssentialFilter;
import play.filters.headers.SecurityHeadersFilter;
import play.http.DefaultHttpFilters;

import javax.inject.Inject;

public class Filters extends DefaultHttpFilters {
    @Inject public Filters(SecurityHeadersFilter securityHeadersFilter) {
        super(securityHeadersFilter);
    }
}
//#filters
