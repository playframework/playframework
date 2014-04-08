/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package detailedtopics.configuration.securityheaders

import play.api.test.{WithApplication, FakeRequest, FakeApplication, PlaySpecification}
import play.filters.headers._
import play.api.mvc.WithFilters
import play.api.GlobalSettings

object SecurityHeaders extends PlaySpecification  {

  "security headers filter" should {
    "be possible to configure in play" in {
      //#global
      import play.api._
      import play.api.mvc._
      import play.filters.headers.SecurityHeadersFilter

      object Global extends WithFilters(SecurityHeadersFilter()) with GlobalSettings {
        // onStart, onStop etc...
      }
      //#global

      running(FakeApplication()) {
        header(SecurityHeadersFilter.X_FRAME_OPTIONS_HEADER,
          Global.doFilter(Action(Results.Ok))(FakeRequest()).run
        ) must beSome(SecurityHeadersFilter.DEFAULT_FRAME_OPTIONS)
      }
    }

    "allow custom settings" in new WithApplication {

      //#custom-config
      val filter = {
         val configuration = play.api.Play.current.configuration
         val securityHeadersConfig:DefaultSecurityHeadersConfig = new SecurityHeadersParser().parse(configuration).asInstanceOf[DefaultSecurityHeadersConfig]
         val sameOriginConfig:SecurityHeadersConfig = securityHeadersConfig.copy(frameOptions = Some("SAMEORIGIN"))
         SecurityHeadersFilter(sameOriginConfig)
      }
      //#custom-config

      object SameOriginGlobal extends WithFilters(filter) with GlobalSettings

      import play.api.mvc._

      header(SecurityHeadersFilter.X_FRAME_OPTIONS_HEADER,
        SameOriginGlobal.doFilter(Action(Results.Ok))(FakeRequest()).run
      ) must beSome("SAMEORIGIN")
    }

  }
}
