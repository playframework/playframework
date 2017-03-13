/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.https

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.{ Application, Configuration }
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.request.RemoteConnection
import play.api.test._

private[https] class TestFilters @Inject() (redirectPlainFilter: RedirectHttpsFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(redirectPlainFilter)
}

class RedirectHttpsFilterSpec extends PlaySpecification {

  "RedirectHttpsFilter" should {

    "redirect when not on https including the path and url query parameters" in new WithApplication(buildApp()) with Injecting {
      val req = request("http", "/please/dont?remove=this&foo=bar")
      val result = route(app, req).get

      status(result) must_== PERMANENT_REDIRECT
      header(LOCATION, result) must_== Some("https://playframework.com/please/dont?remove=this&foo=bar")
    }

    "redirect with custom redirect code if configured" in new WithApplication(buildApp(
      """
        |play.filters.https.redirectCode = 301
      """.stripMargin)) with Injecting {
      val req = request("http", "/please/dont?remove=this&foo=bar")
      val result = route(app, req).get

      status(result) must_== 301
    }

    "not redirect when on https" in new WithApplication(buildApp()) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request("https").withConnection(secure)).get

      header("Strict-Transport-Security", result) must_== None
      status(result) must_== OK
    }

    "contain HSTS header if configured" in new WithApplication(buildApp(
      """
        |play.filters.https.strictTransportSecurity="max-age=31536000; includeSubDomains"
      """.stripMargin)) {
      val secure = RemoteConnection(remoteAddressString = "127.0.0.1", secure = true, clientCertificateChain = None)
      val result = route(app, request("https").withConnection(secure)).get

      header("Strict-Transport-Security", result) must_== Some("max-age=31536000; includeSubDomains")
    }
  }

  private def request(xForwardedProto: String, uri: String = "/")(implicit app: Application) = {
    FakeRequest(method = "GET", path = uri)
      .withHeaders(HOST -> "playframework.com")
  }

  private def buildApp(config: String = "") = new GuiceApplicationBuilder()
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
