/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.headers

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.{ Application, Configuration }
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc.{ DefaultActionBuilder, Result }
import play.api.routing.{ Router, SimpleRouterImpl }
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }

class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter) extends HttpFilters {
  def filters = Seq(securityHeadersFilter)
}

object SecurityHeadersFilterSpec {
  class ResultRouter @Inject() (action: DefaultActionBuilder, result: Result)
    extends SimpleRouterImpl({ case _ => action(result) })
}

class SecurityHeadersFilterSpec extends PlaySpecification {

  import SecurityHeadersFilter._
  import SecurityHeadersFilterSpec._

  sequential

  def configure(rawConfig: String) = {
    val typesafeConfig = ConfigFactory.parseString(rawConfig)
    Configuration(typesafeConfig)
  }

  def withApplication[T](result: Result, config: String)(block: Application => T): T = {
    val app = new GuiceApplicationBuilder()
      .configure(configure(config))
      .overrides(
        bind[Result].to(result),
        bind[Router].to[ResultRouter],
        bind[HttpFilters].to[Filters]
      ).build
    running(app)(block(app))
  }

  "security headers" should {

    "work with default singleton apply method with all default options" in new WithApplication() {
      val filter = SecurityHeadersFilter()
      // Play.current is set at this point...
      val rh = FakeRequest()

      val Action = app.injector.instanceOf[DefaultActionBuilder]
      val action = Action(Ok("success"))
      val result = filter(action)(rh).run()

      header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
      header(X_XSS_PROTECTION_HEADER, result) must beSome("1; mode=block")
      header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("nosniff")
      header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("master-only")
      header(CONTENT_SECURITY_POLICY_HEADER, result) must beNone
      header(REFERRER_POLICY, result) must beSome("origin-when-cross-origin, strict-origin-when-cross-origin")
    }

    "work with singleton apply method using configuration" in new WithApplication() {
      val filter = SecurityHeadersFilter(Configuration.reference)
      val rh = FakeRequest()
      val Action = app.injector.instanceOf[DefaultActionBuilder]
      val action = Action(Ok("success"))
      val result = filter(action)(rh).run()

      header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
      header(X_XSS_PROTECTION_HEADER, result) must beSome("1; mode=block")
      header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("nosniff")
      header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("master-only")
      header(CONTENT_SECURITY_POLICY_HEADER, result) must beNone
      header(REFERRER_POLICY, result) must beSome("origin-when-cross-origin, strict-origin-when-cross-origin")
    }

    "frame options" should {

      "work with custom frame options" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.frameOptions=some frame option
        """.stripMargin) { app =>

          val result = route(app, FakeRequest()).get

          header(X_FRAME_OPTIONS_HEADER, result) must beSome("some frame option")
        }

      "work with no frame options" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.frameOptions=null
        """.stripMargin) { app =>

          val result = route(app, FakeRequest()).get

          header(X_FRAME_OPTIONS_HEADER, result) must beNone
        }
    }

    "xss protection" should {

      "work with custom xss protection" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.xssProtection=some xss protection
        """.stripMargin) { app =>
          val result = route(app, FakeRequest()).get

          header(X_XSS_PROTECTION_HEADER, result) must beSome("some xss protection")
        }

      "work with no xss protection" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.xssProtection=null
        """.stripMargin) { app =>

          val result = route(app, FakeRequest()).get

          header(X_XSS_PROTECTION_HEADER, result) must beNone
        }
    }

    "content type options protection" should {

      "work with custom content type options protection" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.contentTypeOptions="some content type option"
        """.stripMargin) { app =>
          val result = route(app, FakeRequest()).get

          header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("some content type option")
        }

      "work with no content type options protection" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.contentTypeOptions=null
        """.stripMargin) { app =>

          val result = route(app, FakeRequest()).get

          header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beNone
        }
    }

    "permitted cross domain policies" should {

      "work with custom" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.permittedCrossDomainPolicies="some very long word"
        """.stripMargin) { app =>

          val result = route(app, FakeRequest()).get

          header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("some very long word")
        }

      "work with none" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.permittedCrossDomainPolicies=null
        """.stripMargin) { app =>

          val result = route(app, FakeRequest()).get

          header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beNone
        }
    }

    "content security policy protection" should {

      "work with custom" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.contentSecurityPolicy="some content security policy"
        """.stripMargin) { app =>
          val result = route(app, FakeRequest()).get

          header(CONTENT_SECURITY_POLICY_HEADER, result) must beSome("some content security policy")
        }

      "work with none" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.contentSecurityPolicy=null
        """.stripMargin) { app =>

          val result = route(app, FakeRequest()).get

          header(CONTENT_SECURITY_POLICY_HEADER, result) must beNone
        }
    }

    "referrer policy" should {

      "work with custom" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.referrerPolicy="some referrer policy"
        """.stripMargin) { app =>
          val result = route(app, FakeRequest()).get

          header(REFERRER_POLICY, result) must beSome("some referrer policy")
        }

      "work with none" in withApplication(
        Ok("hello"),
        """
          |play.filters.headers.referrerPolicy=null
        """.stripMargin) { app =>

          val result = route(app, FakeRequest()).get

          header(REFERRER_POLICY, result) must beNone
        }
    }

    "action-specific headers" should {
      "use provided header instead of config value if allowActionSpecificHeaders=true in config" in withApplication(
        Ok("hello")
          .withHeaders(REFERRER_POLICY → "my action-specific header"),
        """
          |play.filters.headers.referrerPolicy="some policy"
          |play.filters.headers.allowActionSpecificHeaders=true
        """.stripMargin) { app =>

          val result = route(app, FakeRequest()).get

          header(REFERRER_POLICY, result) must beSome("my action-specific header")
          header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
        }

      "use provided header instead of default if allowActionSpecificHeaders=true in config" in withApplication(
        Ok("hello")
          .withHeaders(REFERRER_POLICY → "my action-specific header"),
        """
          |play.filters.headers.allowActionSpecificHeaders=true
        """.stripMargin) { app =>
          val result = route(app, FakeRequest()).get

          header(REFERRER_POLICY, result) must beSome("my action-specific header")
          header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
        }

      "reject action-specific override if allowActionSpecificHeaders=false in config" in withApplication(
        Ok("hello")
          .withHeaders(REFERRER_POLICY → "my action-specific header"),
        """
          |play.filters.headers.referrerPolicy="some policy"
          |play.filters.headers.allowActionSpecificHeaders=false
        """.stripMargin) { app =>
          val result = route(app, FakeRequest()).get

          // from config
          header(REFERRER_POLICY, result) must beSome("some policy")
          // default
          header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
        }

      "reject action-specific override if allowActionSpecificHeaders is not mentioned in config" in withApplication(
        Ok("hello")
          .withHeaders(REFERRER_POLICY → "my action-specific header"),
        """
          |play.filters.headers.referrerPolicy="some policy"
        """.stripMargin) { app =>
          val result = route(app, FakeRequest()).get

          // from config
          header(REFERRER_POLICY, result) must beSome("some policy")
          // default
          header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
        }
    }
  }
}
