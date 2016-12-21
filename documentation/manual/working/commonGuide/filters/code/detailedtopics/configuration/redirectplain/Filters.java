/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.configuration.hosts;

//#filters
import play.mvc.EssentialFilter;
import play.filters.redirectplain.RedirectPlainFilter;
import play.http.DefaultHttpFilters;

import javax.inject.Inject;

public class Filters extends DefaultHttpFilters {
    @Inject public Filters(RedirectPlainFilter redirectPlainFilter) {
        super(redirectPlainFilter);
    }
}
//#filters
