/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package detailedtopics.configuration.cors.sapi

//#filters
import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.cors.CORSFilter

class Filters @Inject() (corsFilter: CORSFilter) extends HttpFilters {
  def filters = Seq(corsFilter)
}
//#filters
