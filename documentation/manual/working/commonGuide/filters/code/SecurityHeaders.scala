/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package detailedtopics.configuration.securityheaders

//#filters
import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.filters.headers.SecurityHeadersFilter
import play.api.mvc.{ BaseController, ControllerComponents }
//#filters

class SecurityHeaders @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def index = Action {
  //#allowActionSpecificHeaders
  	Ok("Index").withHeaders(SecurityHeadersFilter.REFERRER_POLICY -> "my page-specific header")
  //#allowActionSpecificHeaders
  }
}

object SecurityHeaders {
  class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter) extends DefaultHttpFilters(securityHeadersFilter)
}