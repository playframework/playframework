/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.https

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.{ Configuration, Environment, _ }
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.{ GuiceApplicationBuilder, GuiceableModule }
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.request.RemoteConnection
import play.api.test.{ WithApplication, _ }

private[https] class TestFilters @Inject() (redirectPlainFilter: RedirectHttpsFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(redirectPlainFilter)
}

class RedirectHttpsFilterSpec extends PlaySpecification {

  "RedirectHttpsConfigurationProvider" should {

    "throw configuration error on invalid redirect status code" in {
      val configuration = Configuration.from(Map("play.filters.https.redirectStatusCode" -> "200"))
      val environment = Environment.simple()
      val configProvider = new RedirectHttpsConfigurationProvider(configuration, environment)

      {
        configProvider.get
      } must throwA[com.typesafe.config.ConfigException.Missing]
    }
  }

  "RedirectHttpsFilter" should {

    "redirect when not on https including the path and url query parameters" in new WithApplication(
      buildApp(mode = Mode.Prod)) with Injecting {
      val req = request("/please/dont?remove=this&foo=bar")
      val result = route(app, req).get

      status(result) must_== PERMANENT_REDIRECT
      header(LOCATION, result) must beSome("https://playframework.com/please/dont?remove=this&foo=bar")
    }

    "redirect with custom redirect status code if configured" in new WithApplication(buildApp(
      """
        |play.filters.https.redirectStatusCode = 301
      """.stripMargin, mode = Mode.Prod)) with Injecting {
      val req = request("/please/dont?remove=this&foo=bar")
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
      buildApp("play.filters.https.redirectEnabled = true", mode = Mode.Test)) {
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

    "redirect to custom HTTPS port if configured" in new WithApplication(
      buildApp("play.filters.https.port = 9443", mode = Mode.Prod)) {
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

    "contain custom HSTS header if configured explicitly in prod" in new WithApplication(buildApp(
      """
        |play.filters.https.strictTransportSecurity="max-age=12345; includeSubDomains"
      """.stripMargin, mode = Mode.Prod)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=12345; includeSubDomains")
    }

    "not redirect when xForwardedProtoEnabled is set but no header present" in new WithApplication(buildApp(
      """
        |play.filters.https.redirectEnabled = true
        |play.filters.https.xForwardedProtoEnabled = true
      """.stripMargin, mode = Mode.Test)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== OK
    }
    "redirect when xForwardedProtoEnabled is not set and no header present" in new WithApplication(buildApp(
      """
        |play.filters.https.redirectEnabled = true
        |play.filters.https.xForwardedProtoEnabled = false
      """.stripMargin, mode = Mode.Test)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== PERMANENT_REDIRECT
    }
    "redirect when xForwardedProtoEnabled is set and header is present" in new WithApplication(buildApp(
      """
        |play.filters.https.redirectEnabled = true
        |play.filters.https.xForwardedProtoEnabled = true
      """.stripMargin, mode = Mode.Test)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = false, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure).withHeaders("X-Forwarded-Proto" -> "http")).get

      header(STRICT_TRANSPORT_SECURITY, result) must beNone
      status(result) must_== PERMANENT_REDIRECT
    }
  }

  private def request(uri: String = "/") = {
    FakeRequest(method = "GET", path = uri)
      .withHeaders(HOST -> "playframework.com")
  }

  private def buildApp(config: String = "", mode: Mode = Mode.Test) = GuiceApplicationBuilder(Environment.simple(mode = mode))
    .configure(Configuration(ConfigFactory.parseString(config)))
    .load(
      new play.api.inject.BuiltinModule,
      new play.api.mvc.CookiesModule,
      new play.api.i18n.I18nModule,
      new play.filters.https.RedirectHttpsModule)
    .appRoutes(app => {
      case ("GET", "/") =>
        val action = app.injector.instanceOf[DefaultActionBuilder]
        action(Ok(""))
    }).overrides(
      bind[HttpFilters].to[TestFilters]
    ).build()

}
