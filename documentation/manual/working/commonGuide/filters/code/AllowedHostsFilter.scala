/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.configuration.allowedhosts.sapi

//#filters
import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.filters.hosts.AllowedHostsFilter

class Filters @Inject() (allowedHostsFilter: AllowedHostsFilter)
  extends DefaultHttpFilters(allowedHostsFilter)
//#filters
