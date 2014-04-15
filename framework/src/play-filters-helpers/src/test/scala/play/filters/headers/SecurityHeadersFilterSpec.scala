/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.filters.headers

import play.api.test.{WithApplication, FakeRequest, FakeApplication, PlaySpecification}
import play.api.mvc.{RequestHeader, Action, Result}
import play.api.mvc.Results._
import play.api.Configuration
import scala.concurrent.Future

class SecurityHeadersFilterSpec extends PlaySpecification {

  import SecurityHeadersFilter._

  sequential

  def configure(rawConfig: String) = {
    val typesafeConfig = com.typesafe.config.ConfigFactory.parseString(rawConfig)
    play.api.Configuration(typesafeConfig)
  }

  def defaultConfig: DefaultSecurityHeadersConfig = {
    new SecurityHeadersParser().parse(configure("")).asInstanceOf[DefaultSecurityHeadersConfig]
  }

  def withApplication[T](result: Result, filter: play.api.mvc.Filter)(block: => T): T = {
    running(FakeApplication(withRoutes = {
      case _ => filter.apply(Action(result))
    }))(block)
  }

  "security headers" should {

    "work with default singleton apply method with all default options" in new WithApplication {
      val filter = SecurityHeadersFilter()
      // Play.current is set at this point...
      val rh = FakeRequest()
      val action: (RequestHeader) => Future[Result] = {
        requestHeader =>
          Future.successful(Ok("success"))
      }
      val result = filter(action)(rh)

      header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
      header(X_XSS_PROTECTION_HEADER, result) must beSome("1; mode=block")
      header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("nosniff")
      header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("master-only")
      header(X_CONTENT_SECURITY_POLICY_HEADER, result) must beSome("default-src 'self'")
      header(CONTENT_SECURITY_POLICY_HEADER, result) must beSome("default-src 'self'")
    }

    "work with singleton apply method using configuration" in {
      val typesafeConfig = com.typesafe.config.ConfigFactory.parseString("")
      val config = play.api.Configuration(typesafeConfig)

      val filter = SecurityHeadersFilter(config)
      // Play.current is set at this point...
      val rh = FakeRequest()
      val action: (RequestHeader) => Future[Result] = {
        requestHeader =>
          Future.successful(Ok("success"))
      }
      val result = filter(action)(rh)

      header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
      header(X_XSS_PROTECTION_HEADER, result) must beSome("1; mode=block")
      header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("nosniff")
      header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("master-only")
      header(X_CONTENT_SECURITY_POLICY_HEADER, result) must beSome("default-src 'self'")
      header(CONTENT_SECURITY_POLICY_HEADER, result) must beSome("default-src 'self'")
    }

    "work with new zero argument constructor for Java option" in new WithApplication() {
      // Doesn't use the above construct because we have to be in the scope of the application here.
      val filter = new SecurityHeadersFilter()
      // Play.current is set at this point...
      val rh = FakeRequest()
      val action: (RequestHeader) => Future[Result] = {
        requestHeader =>
          Future.successful(Ok("success"))
      }
      val result = filter(action)(rh)
      header(X_FRAME_OPTIONS_HEADER, result) must beSome("DENY")
      header(X_XSS_PROTECTION_HEADER, result) must beSome("1; mode=block")
      header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("nosniff")
      header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("master-only")
      header(X_CONTENT_SECURITY_POLICY_HEADER, result) must beSome("default-src 'self'")
      header(CONTENT_SECURITY_POLICY_HEADER, result) must beSome("default-src 'self'")
    }

    "frame options" should {

      "work with custom frame options" in withApplication(Ok("hello"), SecurityHeadersFilter(configure(
        """
          |play.filters.headers.frameOptions=some frame option
        """.stripMargin))) {
        val result = route(FakeRequest()).get

        header(X_FRAME_OPTIONS_HEADER, result) must beSome("some frame option")
      }

      "work with no frame options" in withApplication(Ok("hello"), SecurityHeadersFilter(defaultConfig.copy(frameOptions = None))) {
        val result = route(FakeRequest()).get

        header(X_FRAME_OPTIONS_HEADER, result) must beNone
      }
    }

    "xss protection" should {

      "work with custom xss protection" in withApplication(Ok("hello"), SecurityHeadersFilter(configure(
        """
          |play.filters.headers.xssProtection=some xss protection
        """.stripMargin))) {
        val result = route(FakeRequest()).get

        header(X_XSS_PROTECTION_HEADER, result) must beSome("some xss protection")
      }

      "work with no xss protection" in withApplication(Ok("hello"), SecurityHeadersFilter(defaultConfig.copy(xssProtection = None))) {
        val result = route(FakeRequest()).get

        header(X_XSS_PROTECTION_HEADER, result) must beNone
      }
    }

    "content type options protection" should {

      "work with custom content type options protection" in withApplication(Ok("hello"), SecurityHeadersFilter(configure(
        """
          |play.filters.headers.contentTypeOptions="some content type option"
        """.stripMargin))) {
        val result = route(FakeRequest()).get

        header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beSome("some content type option")
      }

      "work with no content type options protection" in withApplication(Ok("hello"), SecurityHeadersFilter(defaultConfig.copy(contentTypeOptions = None))) {
        val result = route(FakeRequest()).get

        header(X_CONTENT_TYPE_OPTIONS_HEADER, result) must beNone
      }
    }

    "permitted cross domain policies" should {

      "work with custom" in withApplication(Ok("hello"), SecurityHeadersFilter(configure(
        """
          |play.filters.headers.permittedCrossDomainPolicies="some very long word"
        """.stripMargin))) {
        val result = route(FakeRequest()).get

        header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beSome("some very long word")
      }

      "work with none" in withApplication(Ok("hello"), SecurityHeadersFilter(defaultConfig.copy(permittedCrossDomainPolicies = None))) {
        val result = route(FakeRequest()).get

        header(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER, result) must beNone
      }
    }

    "content security policy protection" should {

      "work with custom" in withApplication(Ok("hello"), SecurityHeadersFilter(configure(
        """
          |play.filters.headers.contentSecurityPolicy="some content security policy"
        """.stripMargin))) {
        val result = route(FakeRequest()).get

        header(X_CONTENT_SECURITY_POLICY_HEADER, result) must beSome("some content security policy")
        header(CONTENT_SECURITY_POLICY_HEADER, result) must beSome("some content security policy")
      }

      "work with none" in withApplication(Ok("hello"), SecurityHeadersFilter(defaultConfig.copy(contentSecurityPolicy = None))) {
        val result = route(FakeRequest()).get

        header(X_CONTENT_SECURITY_POLICY_HEADER, result) must beNone
        header(CONTENT_SECURITY_POLICY_HEADER, result) must beNone
      }
    }
  }
}
