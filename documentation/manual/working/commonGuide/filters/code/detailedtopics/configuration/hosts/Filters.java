/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.configuration.hosts;

//#filters
import play.mvc.EssentialFilter;
import play.filters.hosts.AllowedHostsFilter;
import play.http.DefaultHttpFilters;

import javax.inject.Inject;

public class Filters extends DefaultHttpFilters {
    @Inject public Filters(AllowedHostsFilter allowedHostsFilter) {
        super(allowedHostsFilter);
    }
}
//#filters
