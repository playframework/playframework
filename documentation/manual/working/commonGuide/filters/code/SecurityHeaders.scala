/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package detailedtopics.configuration.securityheaders

//#filters
import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.filters.headers.SecurityHeadersFilter
//#filters

class SecurityHeaders @Inject() (val controllerComponents: ControllerComponents) extends BaseController {
  def index = Action {
    // #allowActionSpecificHeaders
    Ok("Index").withHeaders(SecurityHeadersFilter.REFERRER_POLICY -> "my page-specific header")
    // #allowActionSpecificHeaders
  }
}

object SecurityHeaders {
  class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter)
      extends DefaultHttpFilters(securityHeadersFilter)
}
