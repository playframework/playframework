/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.filters.headers

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.http.HttpFilters
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.routing.Router
import play.api.test.{ WithApplication, FakeRequest, PlaySpecification }
import play.api.mvc.{ Action, Result }
import play.api.mvc.Results._
import play.api.Configuration
import play.api.inject.bind

object SecurityHeadersFilterSpec extends PlaySpecification {

  import SecurityHeadersFilter._

  sequential

  class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter) extends HttpFilters {
    def filters = Seq(securityHeadersFilter)
  }

  def configure(rawConfig: String) = {
    val typesafeConfig = ConfigFactory.parseString(rawConfig)
    Configuration(typesafeConfig)
  }

  def withApplication[T](result: Result, config: String)(block: => T): T = {
    running(new GuiceApplicationBuilder()
      .configure(configure(config))
      .overrides(
        bind[Router].to(Router.from {
          case _ => Action(result)
        }),
        bind[HttpFilters].to[Filters]
      ).build()
    )(block)
  }

  "security headers" should {

    "work with default singleton apply method with all default options" in new WithApplication {
      val filter = SecurityHeadersFilter()
      // Play.current is set at this point...
      val rh = FakeRequest()
      val action = Action(Ok("success"))
      val result = filter(action)(rh).run

      header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
      header(X_XSS_PROTECTION_HEADER, result) must beSome("1; mode=block")
      header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("nosniff")
      header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("master-only")
      header(CONTENT_SECURITY_POLICY_HEADER, result) must beSome("default-src 'self'")
    }

    "work with singleton apply method using configuration" in {
      val filter = SecurityHeadersFilter(Configuration.reference)
      val rh = FakeRequest()
      val action = Action(Ok("success"))
      val result = filter(action)(rh).run

      header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
      header(X_XSS_PROTECTION_HEADER, result) must beSome("1; mode=block")
      header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("nosniff")
      header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("master-only")
      header(CONTENT_SECURITY_POLICY_HEADER, result) must beSome("default-src 'self'")
    }

    "frame options" should {

      "work with custom frame options" in withApplication(Ok("hello"),
        """
          |play.filters.headers.frameOptions=some frame option
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(X_FRAME_OPTIONS_HEADER, result) must beSome("some frame option")
        }

      "work with no frame options" in withApplication(Ok("hello"),
        """
          |play.filters.headers.frameOptions=null
        """.stripMargin) {

          val result = route(FakeRequest()).get

          header(X_FRAME_OPTIONS_HEADER, result) must beNone
        }
    }

    "xss protection" should {

      "work with custom xss protection" in withApplication(Ok("hello"),
        """
          |play.filters.headers.xssProtection=some xss protection
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(X_XSS_PROTECTION_HEADER, result) must beSome("some xss protection")
        }

      "work with no xss protection" in withApplication(Ok("hello"),
        """
          |play.filters.headers.xssProtection=null
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(X_XSS_PROTECTION_HEADER, result) must beNone
        }
    }

    "content type options protection" should {

      "work with custom content type options protection" in withApplication(Ok("hello"),
        """
          |play.filters.headers.contentTypeOptions="some content type option"
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("some content type option")
        }

      "work with no content type options protection" in withApplication(Ok("hello"),
        """
          |play.filters.headers.contentTypeOptions=null
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beNone
        }
    }

    "permitted cross domain policies" should {

      "work with custom" in withApplication(Ok("hello"),
        """
          |play.filters.headers.permittedCrossDomainPolicies="some very long word"
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("some very long word")
        }

      "work with none" in withApplication(Ok("hello"),
        """
          |play.filters.headers.permittedCrossDomainPolicies=null
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beNone
        }
    }

    "content security policy protection" should {

      "work with custom" in withApplication(Ok("hello"),
        """
          |play.filters.headers.contentSecurityPolicy="some content security policy"
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(CONTENT_SECURITY_POLICY_HEADER, result) must beSome("some content security policy")
        }

      "work with none" in withApplication(Ok("hello"),
        """
          |play.filters.headers.contentSecurityPolicy=null
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(CONTENT_SECURITY_POLICY_HEADER, result) must beNone
        }
    }
  }
}
