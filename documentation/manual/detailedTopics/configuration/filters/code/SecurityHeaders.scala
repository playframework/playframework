/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package detailedtopics.configuration.securityheaders

object SecurityHeaders {

  //#filters
  import javax.inject.Inject

  import play.api.http.HttpFilters
  import play.filters.headers.SecurityHeadersFilter

  class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter) extends HttpFilters {
    def filters = Seq(securityHeadersFilter)
  }
  //#filters
}
