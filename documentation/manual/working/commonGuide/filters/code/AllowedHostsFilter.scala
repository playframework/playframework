/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.configuration.allowedhosts.sapi

//#filters
import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.hosts.AllowedHostsFilter

class Filters @Inject() (allowedHostsFilter: AllowedHostsFilter) extends HttpFilters {
  def filters = Seq(allowedHostsFilter)
}
//#filters
