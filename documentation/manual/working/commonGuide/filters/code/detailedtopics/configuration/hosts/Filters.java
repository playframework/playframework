/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package detailedtopics.configuration.hosts;

//#filters
import play.api.mvc.EssentialFilter;
import play.filters.hosts.AllowedHostsFilter;
import play.http.HttpFilters;

import javax.inject.Inject;

public class Filters implements HttpFilters {

    @Inject
    AllowedHostsFilter allowedHostsFilter;

    public EssentialFilter[] filters() {
        return new EssentialFilter[] { allowedHostsFilter };
    }
}
//#filters
