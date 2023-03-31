/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.ip

import java.net.InetAddress
import java.security.cert.X509Certificate
import javax.inject.Inject

import scala.concurrent.Future

import com.google.common.net.InetAddresses
import com.typesafe.config.ConfigFactory
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.mvc.request.RemoteConnection
import play.api.mvc.Results._
import play.api.routing.HandlerDef
import play.api.routing.Router
import play.api.test._
import play.api.test.WithApplication
import play.api.Application
import play.api.Configuration
import play.api.Environment

private[ip] class TestFilters @Inject() (ipFilter: IPFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(ipFilter)
}

class IPFilterSpec extends PlaySpecification {
  "IPFilter for IPv4 address" should {

    "accept request when ip whitelist and blacklists are empty, which is the default" in new WithApplication(
      buildApp()
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.1")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when ip whitelist and blacklists are explicitly empty" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.1")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when ip whitelist and blacklists are empty which is the default and the routeModifiers white/blacklist are empty too" in new WithApplication(
      // because routeModifiers.whiteList and routeModifiers.blackList are empty a check will take place, but the check says "ip is allowed" because
      // no ip is white or blacklisted
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ ]
                 |play.filters.ip.routeModifiers.whiteList = [ ]
                 |play.filters.ip.routeModifiers.blackList = [ ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.1")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "forbidden request when ip is blacklisted and the routeModifiers white/blacklist are empty" in new WithApplication(
      // because routeModifiers.whiteList and routeModifiers.blackList are empty a check will take place
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "192.168.0.1" ]
                 |play.filters.ip.routeModifiers.whiteList = [ ]
                 |play.filters.ip.routeModifiers.blackList = [ ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.1")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== FORBIDDEN
      }
    }

    "accept request when ip is not blacklisted and the routeModifiers white/blacklist are empty" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "192.168.0.2" ]
                 |play.filters.ip.routeModifiers.whiteList = [ ]
                 |play.filters.ip.routeModifiers.blackList = [ ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.1")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when IP isn't whitelisted and it's an excluded path" in new WithApplication(
      // default: play.filters.ip.routeModifiers.whiteList = [ "anyip" ]
      buildApp("""
                 |play.filters.ip.whiteList = []
      """.stripMargin)
    ) with Injecting {
      override def running() = {
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
              Seq("anyip")
            )
          )
        val result: Future[Result] = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when IP is not whitelisted but it's an excluded path" in new WithApplication(
      // default: play.filters.ip.routeModifiers.whiteList = [ "anyip" ]
      buildApp("""
                 |play.filters.ip.whiteList = [ "192.167.0.3" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
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
              Seq("anyip")
            )
          )
        val result: Future[Result] = route(app, req).get

        status(result) must_== OK
      }
    }

    "forbidden request because the route does get explicitly checked and the IP is blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "192.168.0.3" ]
                 |play.filters.ip.routeModifiers.whiteList = [ ]
                 |play.filters.ip.routeModifiers.blackList = [ "checkip" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
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
              Seq("checkip")
            )
          )
        val result: Future[Result] = route(app, req).get

        status(result) must_== FORBIDDEN
      }
    }

    // same test again, but the route definition does not have a route modifier
    "accept request because the routes does NOT get explicitly checked and it does not matter that the IP is blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "192.168.0.3" ]
                 |play.filters.ip.routeModifiers.whiteList = [ ]
                 |play.filters.ip.routeModifiers.blackList = [ "checkip" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
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
              Seq() // <-- we don't tell the route to check the IP, so there will be no check
            )
          )
        val result: Future[Result] = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when IP is whitelisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "192.168.0.1" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/my-excluded-path", "192.168.0.1")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when IP isn't whitelisted and also not blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "192.168.0.1" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.2")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "forbidden request when IP isn't whitelisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "192.168.0.100" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.2")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== FORBIDDEN
      }
    }

    "forbidden request when IP isn't whitelisted but it's blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "192.168.0.1" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.1")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== FORBIDDEN
      }
    }

    "401 http status code when IP isn't whitelisted with custom http status code" in new WithApplication(
      buildApp("""
                 |play.filters.ip.accessDeniedHttpStatusCode = 401
                 |play.filters.ip.whiteList = [ "192.168.0.1" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "192.168.0.2")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== UNAUTHORIZED
      }
    }
  }

  "IPFilter for IPv6 address" should {
    "accept request when ip whitelist and blacklist are empty which is the default" in new WithApplication(
      buildApp()
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "8f:f3b:0:0:0:0:0:ff")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when ip whitelist and blacklist are explicitly empty" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ ]
        """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "8f:f3b:0:0:0:0:0:ff")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when IP isn't whitelisted and it's an excluded path" in new WithApplication(
      // default: play.filters.ip.routeModifiers.whiteList = [ "anyip" ]
      buildApp("""
                 |play.filters.ip.whiteList = []
      """.stripMargin)
    ) with Injecting {
      override def running() = {
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
              Seq("anyip")
            )
          )
        val result: Future[Result] = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when IP is not whitelisted but it's an excluded path" in new WithApplication(
      // default: play.filters.ip.routeModifiers.whiteList = [ "anyip" ]
      buildApp("""
                 |play.filters.ip.whiteList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: Request[AnyContentAsEmpty.type] = request("/my-excluded-path", "8f:f2b:0:0:0:0:0:ff")
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
              Seq("anyip")
            )
          )
        val result: Future[Result] = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when IP is whitelisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/my-excluded-path", "8f:f3b:0:0:0:0:0:ff")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when IP isn't whitelisted and also not blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "ff:ffb:0:0:0:0:0:ff")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "forbidden request when IP isn't whitelisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "ff:ffb:0:0:0:0:0:ff")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== FORBIDDEN
      }
    }

    "forbidden request when IP isn't whitelisted but it's blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ ]
                 |play.filters.ip.blackList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "8f:f3b:0:0:0:0:0:ff")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== FORBIDDEN
      }
    }

    "401 http status code when IP isn't whitelisted with custom http status code" in new WithApplication(
      buildApp("""
                 |play.filters.ip.accessDeniedHttpStatusCode = 401
                 |play.filters.ip.whiteList = [ "8f:f3b:0:0:0:0:0:ff" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "ff:ffb:0:0:0:0:0:ff")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== UNAUTHORIZED
      }
    }

    "forbidden request when IP isn't whitelisted, whitelisted IP written in short from notation)" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "2001:cdba::3257:9652" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "2001:cdba:0:0:0:0:3257:9653")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== FORBIDDEN
      }
    }

    "accept request when IP is whitelisted, whitelisted IP written in short from notation)" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "2001:cdba::3257:9652" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "2001:cdba:0:0:0:0:3257:9652")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "accept request when IP is whitelisted, whitelisted IP written in short from notation with zeros)" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "2001:cdba:0000:0000:0000:0000:3257:9652" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "2001:cdba:0:0:0:0:3257:9652")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== OK
      }
    }

    "forbidden request when IP is blacklisted, blacklisted IP written in short from notation)" in new WithApplication(
      buildApp("""
                 |play.filters.ip.blackList = [ "2001:cdba::3257:9652" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "2001:cdba:0:0:0:0:3257:9652")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== FORBIDDEN
      }
    }

    "forbidden request when IP is blacklisted, blacklisted IP written in short from notation with zeros)" in new WithApplication(
      buildApp("""
                 |play.filters.ip.blackList = [ "2001:cdba:0000:0000:0000:0000:3257:9652" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val req: FakeRequest[AnyContentAsEmpty.type] = request("/", "2001:cdba:0:0:0:0:3257:9652")
        val result: Future[Result]                   = route(app, req).get

        status(result) must_== FORBIDDEN
      }
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
