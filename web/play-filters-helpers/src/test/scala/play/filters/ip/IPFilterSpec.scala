/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
import play.api.Application
import play.api.Configuration
import play.api.Environment

import scala.concurrent.Future

private[ip] class TestFilters @Inject() (ipFilter: IPFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(ipFilter)
}

class IPFilterSpec extends PlaySpecification {
  "IPFilter for IPv4 address" should {
    "accept request when IP isn't whitelisted but it's an excluded path" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = []
      """.stripMargin)
    ) with Injecting {
      val req: Request[AnyContentAsEmpty.type] = request("/my-excluded-path", "192.168.0.2")
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
      val result: Future[Result] = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP is whitelisted but it's an excluded path" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ""192.167.0.3"" ]
      """.stripMargin)
    ) with Injecting {
      val req: Request[AnyContentAsEmpty.type] = request("/my-excluded-path", "192.168.0.3")
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
      val result: Future[Result] = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP is whitelisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ""192.168.0.1"" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/my-excluded-path", "192.168.0.1")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP isn't whitelisted and blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ ""192.168.0.1"" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.2")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== OK
    }

    "forbidden request when IP isn't whitelisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ""192.168.0.100"" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.2")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== FORBIDDEN
    }

    "forbidden request when IP isn't whitelisted but it's blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ ""192.168.0.1"" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.1")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== FORBIDDEN
    }

    "not found request when IP isn't whitelisted with custom http status code" in new WithApplication(
      buildApp("""
                 |play.filters.ip.httpStatusCode = 404
                 |play.filters.ip.whiteList = [ ""192.168.0.1"" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.2")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== NOT_FOUND
    }
  }

  "IPFilter for IPv6 address" should {

    "accept request when IP isn't whitelisted but it's an excluded path" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = []
      """.stripMargin)
    ) with Injecting {
      val req: Request[AnyContentAsEmpty.type] = request("/my-excluded-path", "8f:f3b:0:0:0:0:0:ff")
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
      val result: Future[Result] = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP is whitelisted but it's an excluded path" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      val req: Request[AnyContentAsEmpty.type] = request("/my-excluded-path", "8f:f3b:0:0:0:0:0:ff")
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
      val result: Future[Result] = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP is whitelisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/my-excluded-path", "8f:f3b:0:0:0:0:0:ff")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== OK
    }

    "accept request when IP isn't whitelisted and blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "ff:ffb:0:0:0:0:0:ff")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== OK
    }

    "forbidden request when IP isn't whitelisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "ff:ffb:0:0:0:0:0:ff")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== FORBIDDEN
    }

    "forbidden request when IP isn't whitelisted but it's blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "8f:f3b:0:0:0:0:0:ff")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== FORBIDDEN
    }

    "not found request when IP isn't whitelisted with custom http status code" in new WithApplication(
      buildApp("""
                 |play.filters.ip.httpStatusCode = 404
                 |play.filters.ip.whiteList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "ff:ffb:0:0:0:0:0:ff")
      val result: Future[Result]                   = route(app, req).get

      status(result) must_== NOT_FOUND
    }
  }

  private def request(path: String, ip: String): FakeRequest[AnyContentAsEmpty.type] = {
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

  private def buildApp(config: String = ""): Application =
    GuiceApplicationBuilder(Environment.simple())
      .configure(Configuration(ConfigFactory.parseString(config)))
      .load(
        new play.api.inject.BuiltinModule,
        new play.api.mvc.CookiesModule,
        new play.api.i18n.I18nModule,
        new play.filters.ip.IPFilterModule
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
