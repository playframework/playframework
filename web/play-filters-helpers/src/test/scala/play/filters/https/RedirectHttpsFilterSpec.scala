/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.https

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.Environment
import play.api._
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.request.RemoteConnection
import play.api.test.WithApplication
import play.api.test._

private[https] class TestFilters @Inject() (redirectPlainFilter: RedirectHttpsFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(redirectPlainFilter)
}

class RedirectHttpsFilterSpec extends PlaySpecification {
  "RedirectHttpsConfigurationProvider" should {
    "throw configuration error on invalid redirect status code" in {
      val configuration  = Configuration.from(Map("play.filters.https.redirectStatusCode" -> "200"))
      val environment    = Environment.simple()
      val configProvider = new RedirectHttpsConfigurationProvider(configuration, environment)

      {
        configProvider.get
      } must throwA[com.typesafe.config.ConfigException.Missing]
    }
  }

  "RedirectHttpsFilter" should {
    "redirect when not on https including the path and url query parameters" in new WithApplication(
      buildApp(mode = Mode.Prod)
    ) with Injecting {
      val req    = request("/please/dont?remove=this&foo=bar")
      val result = route(app, req).get

      status(result) must_== PERMANENT_REDIRECT
      header(LOCATION, result) must beSome("https://playframework.com/please/dont?remove=this&foo=bar")
    }

    "redirect with custom redirect status code if configured" in new WithApplication(
      buildApp("""
                 |play.filters.https.redirectStatusCode = 301
      """.stripMargin, mode = Mode.Prod)
    ) with Injecting {
      val req    = request("/please/dont?remove=this&foo=bar")
      val result = route(app, req).get

      status(result) must_== 301
    }

    "not redirect when on http in test" in new WithApplication(buildApp(mode = Mode.Test)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== OK
    }

    "redirect when on http in test and redirectEnabled = true" in new WithApplication(
      buildApp("play.filters.https.redirectEnabled = true", mode = Mode.Test)
    ) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== PERMANENT_REDIRECT
    }

    "not redirect when on https but send HSTS header" in new WithApplication(buildApp(mode = Mode.Prod)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; includeSubDomains")
      status(result) must_== OK
    }

    "not redirect when X-Forwarded-Proto header is 'https' (and not on https) but send HSTS header" in new WithApplication(
      buildApp(
        """
          |play.filters.https.xForwardedProtoEnabled = true
      """.stripMargin,
        mode = Mode.Prod
      )
    ) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure).withHeaders("X-Forwarded-Proto" -> "https")).get

      header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; includeSubDomains")
      status(result) must_== OK
    }

    "redirect to custom HTTPS port if configured" in new WithApplication(
      buildApp("play.filters.https.port = 9443", mode = Mode.Prod)
    ) {
      val result = route(app, request("/please/dont?remove=this&foo=bar")).get

      header(LOCATION, result) must beSome("https://playframework.com:9443/please/dont?remove=this&foo=bar")
    }

    "not contain default HSTS header if secure in test" in new WithApplication(buildApp(mode = Mode.Test)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
    }

    "contain default HSTS header if secure in production" in new WithApplication(buildApp(mode = Mode.Prod)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; includeSubDomains")
    }

    "contain custom HSTS header if configured explicitly in prod" in new WithApplication(
      buildApp("""
                 |play.filters.https.strictTransportSecurity="max-age=12345; includeSubDomains"
      """.stripMargin, mode = Mode.Prod)
    ) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=12345; includeSubDomains")
    }

    "not redirect when xForwardedProtoEnabled is set but no header present" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
      """.stripMargin,
        mode = Mode.Test
      )
    ) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== OK
    }
    "redirect when xForwardedProtoEnabled is not set and no header present" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = false
      """.stripMargin,
        mode = Mode.Test
      )
    ) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== PERMANENT_REDIRECT
    }
    "redirect when xForwardedProtoEnabled is set and header is present" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
      """.stripMargin,
        mode = Mode.Test
      )
    ) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure).withHeaders("X-Forwarded-Proto" -> "http")).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== PERMANENT_REDIRECT
    }

    "send HSTS header when request itself is not secure but X-Forwarded-Proto header is 'https'" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
      """.stripMargin,
        mode = Mode.Test
      )
    ) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure).withHeaders("X-Forwarded-Proto" -> "https")).get

      header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; includeSubDomains")
      status(result) must_== OK
    }

    "not redirect when path included in redirectExcludePath" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
          |play.filters.https.excludePaths = ["/skip"]
      """.stripMargin,
        mode = Mode.Test
      )
    ) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request("/skip").withConnection(secure).withHeaders("X-Forwarded-Proto" -> "http")).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== OK
    }

    "not redirect when path included in redirectExcludePath and request has query params" in new WithApplication(
      buildApp(
        """
          |play.filters.https.redirectEnabled = true
          |play.filters.https.xForwardedProtoEnabled = true
          |play.filters.https.excludePaths = ["/skip"]
      """.stripMargin,
        mode = Mode.Test
      )
    ) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(
        app,
        request("/skip", Some("foo=bar")).withConnection(secure).withHeaders("X-Forwarded-Proto" -> "http")
      ).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== OK
    }
  }

  private def request(path: String = "/", queryParams: Option[String] = None) = {
    FakeRequest(method = "GET", path = path + queryParams.map("?" + _).getOrElse(""))
      .withHeaders(HOST -> "playframework.com")
  }

  private def buildApp(config: String = "", mode: Mode = Mode.Test) =
    GuiceApplicationBuilder(Environment.simple(mode = mode))
      .configure(Configuration(ConfigFactory.parseString(config)))
      .load(
        new play.api.inject.BuiltinModule,
        new play.api.mvc.CookiesModule,
        new play.api.i18n.I18nModule,
        new play.filters.https.RedirectHttpsModule
      )
      .appRoutes(app => {
        case ("GET", "/") =>
          val action = app.injector.instanceOf[DefaultActionBuilder]
          action(Ok(""))
        case ("GET", "/skip") =>
          val action = app.injector.instanceOf[DefaultActionBuilder]
          action(Ok(""))
      })
      .overrides(
        bind[HttpFilters].to[TestFilters]
      )
      .build()
}
