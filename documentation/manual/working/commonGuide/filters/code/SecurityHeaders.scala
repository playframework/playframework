/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

package detailedtopics.configuration.securityheaders

object SecurityHeaders {

  //#filters
  import javax.inject.Inject

  import play.api.http.DefaultHttpFilters
  import play.filters.headers.SecurityHeadersFilter

  class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter) extends DefaultHttpFilters(securityHeadersFilter)
  //#filters
}
