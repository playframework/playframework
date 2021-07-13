/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.ip

import java.net.InetAddress
import java.security.cert.X509Certificate

import com.google.common.net.InetAddresses
import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc._
import play.api.mvc.request.RemoteConnection
import play.api.routing.HandlerDef
import play.api.routing.Router
import play.api.test.WithApplication
import play.api.test._
import play.api.Configuration
import play.api.Environment

private[ip] class TestFilters @Inject() (allowedIPFilter: AllowedIPFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(allowedIPFilter)
}

class AllowedIPFilterSpec extends PlaySpecification {
  "AllowedIPFilter for IPv4 address" should {
    "accept request when IP Filter is disabled" in new WithApplication(
      buildApp("play.filters.ip.enabled = false")
    ) with Injecting {
      val req    = request("/", "192.168.0.2")
      val result = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP isn't allowed but it's an excluded path" in new WithApplication(
      buildApp("""
                 |play.filters.ip.enabled = true
                 |play.filters.ip.allowList = []
      """.stripMargin)
    ) with Injecting {
      val req = request("/my-excluded-path", "192.168.0.2")
        .addAttr(
          Router.Attrs.HandlerDef,
          HandlerDef(
            app.classloader,
            "routes",
            "FooController",
            "foo",
            Seq.empty,
            "GET",
            "/my-excluded-path",
            "comments",
            Seq("noipcheck")
          )
        )
      val result = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP is allowed but it's an excluded path" in new WithApplication(
      buildApp("""
                 |play.filters.ip.enabled = true
                 |play.filters.ip.allowList = [ ""192.167.0.3"" ]
      """.stripMargin)
    ) with Injecting {
      val req = request("/my-excluded-path", "192.168.0.3")
        .addAttr(
          Router.Attrs.HandlerDef,
          HandlerDef(
            app.classloader,
            "routes",
            "FooController",
            "foo",
            Seq.empty,
            "GET",
            "/my-excluded-path",
            "comments",
            Seq("noipcheck")
          )
        )
      val result = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP is allowed" in new WithApplication(
      buildApp("""
                 |play.filters.ip.enabled = true
                 |play.filters.ip.allowList = [ ""192.168.0.1"" ]
      """.stripMargin)
    ) with Injecting {
      val req    = request("/my-excluded-path", "192.168.0.1")
      val result = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP is allowed" in new WithApplication(
      buildApp("""
                 |play.filters.ip.enabled = true
                 |play.filters.ip.allowList = [ ""192.168.0.1"" ]
      """.stripMargin)
    ) with Injecting {
      val req    = request("/my-excluded-path", "192.168.0.1")
      val result = route(app, req).get

      status(result) must_== OK
    }

    "forbidden request when IP isn't allowed" in new WithApplication(
      buildApp("play.filters.ip.allowList = []")
    ) with Injecting {
      val req    = request("/", "192.168.0.2")
      val result = route(app, req).get

      status(result) must_== FORBIDDEN
    }

  }

  "AllowedIPFilter for IPv6 address" should {
    "accept request when IP Filter is disabled" in new WithApplication(
      buildApp("play.filters.ip.enabled = false")
    ) with Injecting {
      val req    = request("/", "8F:F3B::FF")
      val result = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP isn't allowed but it's an excluded path" in new WithApplication(
      buildApp("""
                 |play.filters.ip.enabled = true
                 |play.filters.ip.allowList = []
      """.stripMargin)
    ) with Injecting {
      val req = request("/my-excluded-path", "8F:F3B::FF")
        .addAttr(
          Router.Attrs.HandlerDef,
          HandlerDef(
            app.classloader,
            "routes",
            "FooController",
            "foo",
            Seq.empty,
            "GET",
            "/my-excluded-path",
            "comments",
            Seq("noipcheck")
          )
        )
      val result = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP is allowed but it's an excluded path" in new WithApplication(
      buildApp("""
                 |play.filters.ip.enabled = true
                 |play.filters.ip.allowList = [ "8F:F3B::FF" ]
      """.stripMargin)
    ) with Injecting {
      val req = request("/my-excluded-path", "8F:F3B::FF")
        .addAttr(
          Router.Attrs.HandlerDef,
          HandlerDef(
            app.classloader,
            "routes",
            "FooController",
            "foo",
            Seq.empty,
            "GET",
            "/my-excluded-path",
            "comments",
            Seq("noipcheck")
          )
        )
      val result = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP is allowed" in new WithApplication(
      buildApp("""
                 |play.filters.ip.enabled = true
                 |play.filters.ip.allowList = [ "8F:F3B::FF" ]
      """.stripMargin)
    ) with Injecting {
      val req    = request("/", "8F:F3B::FF")
      val result = route(app, req).get

      status(result) must_== OK
    }

    "forbidden request when IP isn't allowed" in new WithApplication(
      buildApp("play.filters.ip.allowList = []")
    ) with Injecting {
      val req    = request("/", "8F:F3B::FF")
      val result = route(app, req).get

      status(result) must_== FORBIDDEN
    }

  }

  private def request(path: String, ip: String) = {
    FakeRequest(method = "GET", path = path)
      .withConnection(new RemoteConnection {

        /**
         * The remote client's address.
         */
        override def remoteAddress: InetAddress = InetAddresses.forString(ip)

        /**
         * Whether or not the connection was over a secure (e.g. HTTPS) connection.
         */
        override def secure: Boolean = false

        /**
         * The X509 certificate chain presented by a client during SSL requests.
         */
        override def clientCertificateChain: Option[Seq[X509Certificate]] = Option.empty
      })
      .withHeaders(HOST -> "playframework.com")
  }

  private def buildApp(config: String = "") =
    GuiceApplicationBuilder(Environment.simple())
      .configure(Configuration(ConfigFactory.parseString(config)))
      .load(
        new play.api.inject.BuiltinModule,
        new play.api.mvc.CookiesModule,
        new play.api.i18n.I18nModule,
        new play.filters.ip.AllowedIPModule
      )
      .appRoutes(app => {
        case ("GET", "/") =>
          val action = app.injector.instanceOf[DefaultActionBuilder]
          action(Ok(""))
        case ("GET", "/my-excluded-path") =>
          val action = app.injector.instanceOf[DefaultActionBuilder]
          action(Ok(""))
      })
      .overrides(
        bind[HttpFilters].to[TestFilters]
      )
      .build()
}
