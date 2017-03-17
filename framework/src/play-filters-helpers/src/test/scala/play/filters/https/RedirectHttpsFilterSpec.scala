/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.https

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.Mode.Mode
import play.api._
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
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

    "redirect when not on https including the path and url query parameters" in new WithApplication(buildApp()) with Injecting {
      val req = request("/please/dont?remove=this&foo=bar")
      val result = route(app, req).get

      status(result) must_== PERMANENT_REDIRECT
      header(LOCATION, result) must_== Some("https://playframework.com/please/dont?remove=this&foo=bar")
    }

    "redirect with custom redirect status code if configured" in new WithApplication(buildApp(
      """
        |play.filters.https.redirectStatusCode = 301
      """.stripMargin)) with Injecting {
      val req = request("/please/dont?remove=this&foo=bar")
      val result = route(app, req).get

      status(result) must_== 301
    }

    "not redirect when on https" in new WithApplication(buildApp()) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must_== None
      status(result) must_== OK
    }

    "redirect to custom HTTPS port if configured" in new WithApplication(buildApp("play.filters.https.port = 9443")) {
      val result = route(app, request("/please/dont?remove=this&foo=bar")).get

      header(LOCATION, result) must_== Some("https://playframework.com:9443/please/dont?remove=this&foo=bar")
    }

    "not contain default HSTS header if secure in test" in new WithApplication(buildApp()) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must_== None
    }

    "contain default HSTS header if secure in production" in new WithApplication(buildApp(mode = Mode.Prod)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must_== Some("max-age=31536000; includeSubDomains")
    }

    "contain custom HSTS header if configured explicitly in prod" in new WithApplication(buildApp(
      """
        |play.filters.https.strictTransportSecurity="max-age=12345; includeSubDomains"
      """.stripMargin, mode = Mode.Prod)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request().withConnection(secure)).get

      header(STRICT_TRANSPORT_SECURITY, result) must_== Some("max-age=12345; includeSubDomains")
    }
  }

  private def request(uri: String = "/") = {
    FakeRequest(method = "GET", path = uri)
      .withHeaders(HOST -> "playframework.com")
  }

  private def buildApp(config: String = "", mode: Mode = Mode.Test) = GuiceApplicationBuilder(Environment.simple(mode = mode))
    .configure(Configuration(ConfigFactory.parseString(config)))
    .appRoutes(app => {
      case ("GET", "/") =>
        val action = app.injector.instanceOf[DefaultActionBuilder]
        action(Ok(""))
    })
    .overrides(
      bind[RedirectHttpsConfiguration].toProvider[RedirectHttpsConfigurationProvider],
      bind[HttpFilters].to[TestFilters]
    ).build()

}
