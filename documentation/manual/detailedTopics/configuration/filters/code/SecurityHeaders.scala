/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package detailedtopics.configuration.securityheaders

import play.api.test.{WithApplication, PlaySpecification}
import play.filters.headers.{SecurityHeadersParser, DefaultSecurityHeadersConfig}

object SecurityHeaders extends PlaySpecification  {

  //#filters
  import javax.inject.Inject

  import play.api.http.HttpFilters
  import play.filters.headers.SecurityHeadersFilter

  class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter) extends HttpFilters {
    def filters = Seq(securityHeadersFilter)
  }
  //#filters

  "security headers filter" should {

    "allow custom settings" in new WithApplication {

      //#custom-config
      val filter = {
         val configuration = play.api.Play.current.configuration
         val securityHeadersConfig = new SecurityHeadersParser().parse(configuration).asInstanceOf[DefaultSecurityHeadersConfig]
         val sameOriginConfig = securityHeadersConfig.copy(frameOptions = Some("SAMEORIGIN"))
         SecurityHeadersFilter(sameOriginConfig)
      }
      //#custom-config

      ok
    }

  }
}
