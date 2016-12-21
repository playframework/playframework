/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.redirectplain

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.test._
import play.api.{ Application, Configuration }

private[redirectplain] class TestFilters @Inject() (redirectPlainFilter: RedirectPlainFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(redirectPlainFilter)
}

class RedirectPlainFilterSpec extends PlaySpecification {

  private def request(app: Application, xForwardedProto: String, uri: String = "/", headers: Seq[(String, String)] = Seq()) = {
    val req = FakeRequest(method = "GET", path = uri)
      .withHeaders(headers: _*)
      .withHeaders(HOST -> "playframework.com")
      .withHeaders(X_FORWARDED_PROTO -> xForwardedProto)
    route(app, req).get
  }

  def buildApp(config: String) = new GuiceApplicationBuilder()
    .configure(Configuration(ConfigFactory.parseString(config)))
    .overrides(
      bind[Router].to(Router.from {
        case _ => Action(Ok(""))
      }),
      bind[RedirectPlainConfig].toProvider[RedirectPlainConfigProvider],
      bind[HttpFilters].to[TestFilters]
    ).build

  def withApplication[T](config: String)(block: Application => T): T = {
    val app = buildApp(config)
    running(app)(block(app))
  }

  "RedirectPlainFilter" should {

    "correctly determine X-Forwarded-Proto security" in {
      RedirectPlainFilter.isSecureForwarded(FakeRequest().withHeaders(X_FORWARDED_PROTO -> "http")) must_== false
      RedirectPlainFilter.isSecureForwarded(FakeRequest().withHeaders(X_FORWARDED_PROTO -> "https")) must_== true
    }

    "correctly determine Forwarded security" in {
      RedirectPlainFilter.isSecureForwarded(FakeRequest().withHeaders(FORWARDED -> "for=192.0.2.60;proto=http;by=203.0.113.43")) must_== false
      RedirectPlainFilter.isSecureForwarded(FakeRequest().withHeaders(FORWARDED -> "for=192.0.2.60;proto=https;by=203.0.113.43")) must_== true
    }

    "create a proper redirect url" in {
      val request = FakeRequest(GET, "/please/dont?remove=this&foo=bar").withHeaders("Host" -> "playframework.com")

      RedirectPlainFilter.createHttpsRedirectUrl(request) must_== "https://playframework.com/please/dont?remove=this&foo=bar"
    }

    "redirect when not on https including the path and url query parameters" in withApplication("") { app =>
      val req = request(app, "http", "/please/dont?remove=this&foo=bar")

      status(req) must_== PERMANENT_REDIRECT
      header(LOCATION, req) must_== Some("https://playframework.com/please/dont?remove=this&foo=bar")
    }

    "not redirect when on https" in withApplication("") { app =>
      status(request(app, "https")) must_== OK
    }

    "add strict transport security headers on https" in withApplication("play.filters.redirectplain.strict-transport-security.max-age=12345") { app =>
      header(STRICT_TRANSPORT_SECURITY, request(app, "https")) must_== Some ("max-age=12345")
    }
  }

}
