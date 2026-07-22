/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.ip

import scala.concurrent.Future
import scala.util.Try

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import jakarta.inject.Inject
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.mvc.request.NodePort
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RemoteNode
import play.api.mvc.request.RequestAuthority
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

  "IPFilter for non-IP remote nodes" should {
    "allow unknown and obfuscated nodes when an unrelated IP is blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.blackList = [ "192.0.2.1" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo(RemoteNode.Unknown(None), None),
          RemoteInfo(RemoteNode.Obfuscated("_hidden", None), None)
        ) must_== Seq(OK, OK)
      }
    }

    "match unknown case-insensitively when it is blacklisted" in new WithApplication(
      buildApp("""
                 |play.filters.ip.blackList = [ "UnKnOwN" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo(RemoteNode.Unknown(None), None),
          RemoteInfo(RemoteNode.Obfuscated("_hidden", None), None),
          RemoteInfo.ip("192.0.2.1", None)
        ) must_== Seq(FORBIDDEN, OK, OK)
      }
    }

    "match unknown case-insensitively and fail closed when it is the whitelist" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "UNKNOWN" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo(RemoteNode.Unknown(None), None),
          RemoteInfo(RemoteNode.Obfuscated("_hidden", None), None),
          RemoteInfo.ip("192.0.2.1", None)
        ) must_== Seq(OK, FORBIDDEN, FORBIDDEN)
      }
    }

    "match blacklisted obfuscated identifiers exactly and case-sensitively" in new WithApplication(
      buildApp("""
                 |play.filters.ip.blackList = [ "_edge-1.v2" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo(RemoteNode.Obfuscated("_edge-1.v2", None), None),
          RemoteInfo(RemoteNode.Obfuscated("_EDGE-1.v2", None), None),
          RemoteInfo(RemoteNode.Obfuscated("_edge-1.v20", None), None)
        ) must_== Seq(FORBIDDEN, OK, OK)
      }
    }

    "match whitelisted obfuscated identifiers exactly and case-sensitively" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "_edge" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo(RemoteNode.Obfuscated("_edge", None), None),
          RemoteInfo(RemoteNode.Obfuscated("_EDGE", None), None),
          RemoteInfo(RemoteNode.Obfuscated("_edge-2", None), None)
        ) must_== Seq(OK, FORBIDDEN, FORBIDDEN)
      }
    }

    "allow every remote node type when both lists are empty" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = []
                 |play.filters.ip.blackList = []
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("192.0.2.1", None),
          RemoteInfo(RemoteNode.Unknown(None), None),
          RemoteInfo(RemoteNode.Obfuscated("_hidden", None), None)
        ) must_== Seq(OK, OK, OK)
      }
    }

    "forbid unknown and obfuscated nodes when a whitelist is configured" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "192.168.0.100" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val unknownResult    = route(app, request("/", RemoteNode.Unknown(None))).get
        val obfuscatedResult =
          route(app, request("/", RemoteNode.Obfuscated("_hidden", None))).get

        status(unknownResult) must_== FORBIDDEN
        status(obfuscatedResult) must_== FORBIDDEN
      }
    }

    "match only the selected node and ignore its port and by node" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "192.0.2.0/24", "unknown", "_edge" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val ipWithPortAndBy = RemoteInfo
          .ip("192.0.2.8", Some(NodePort.Numeric(443)))
          .copy(byNode = Some(RemoteNode.Obfuscated("_proxy", Some(NodePort.Obfuscated("_listener")))))
        val obfuscatedWithPortAndBy = RemoteInfo(
          RemoteNode.Obfuscated("_edge", Some(NodePort.Obfuscated("_source"))),
          Some(RemoteNode.Unknown(Some(NodePort.Numeric(8443))))
        )
        val unknownWithPortAndBy = RemoteInfo(
          RemoteNode.Unknown(Some(NodePort.Numeric(1234))),
          Some(RemoteInfo.ip("203.0.113.10", None).node)
        )
        val nonMatchingIdentityWithMatchingBy = RemoteInfo(
          RemoteNode.Obfuscated("_other", None),
          Some(RemoteNode.Obfuscated("_edge", None))
        )
        val nonMatchingIpWithMatchingBy = RemoteInfo
          .ip("198.51.100.10", None)
          .copy(byNode = Some(RemoteInfo.ip("192.0.2.9", None).node))

        remoteStatuses(
          app,
          ipWithPortAndBy,
          obfuscatedWithPortAndBy,
          unknownWithPortAndBy,
          nonMatchingIdentityWithMatchingBy,
          nonMatchingIpWithMatchingBy
        ) must_== Seq(OK, OK, OK, FORBIDDEN, FORBIDDEN)
      }
    }

    "bypass identity checks for non-IP nodes on a route excluded by a modifier" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "192.0.2.1" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        val unknownRequest = requestWithRouteModifiers(
          "/my-excluded-path",
          RemoteInfo(RemoteNode.Unknown(None), None),
          Seq("anyip")
        )
        val obfuscatedRequest = requestWithRouteModifiers(
          "/my-excluded-path",
          RemoteInfo(RemoteNode.Obfuscated("_hidden", None), None),
          Seq("anyip")
        )

        Seq(
          status(route(app, unknownRequest).get),
          status(route(app, obfuscatedRequest).get)
        ) must_== Seq(OK, OK)
      }
    }

    "let a non-empty whitelist take precedence over the blacklist for every node type" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "_edge" ]
                 |play.filters.ip.blackList = [ "_edge", "unknown", "192.0.2.1" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo(RemoteNode.Obfuscated("_edge", None), None),
          RemoteInfo(RemoteNode.Unknown(None), None),
          RemoteInfo.ip("192.0.2.1", None),
          RemoteInfo(RemoteNode.Obfuscated("_not-listed-anywhere", None), None)
        ) must_== Seq(OK, FORBIDDEN, FORBIDDEN, FORBIDDEN)
      }
    }
  }

  "IPFilter CIDR entries" should {
    "match IPv4 CIDRs in the whitelist, including CIDRs with host bits" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "10.1.2.3/8" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("10.255.254.253", None),
          RemoteInfo.ip("11.0.0.1", None)
        ) must_== Seq(OK, FORBIDDEN)
      }
    }

    "match IPv6 CIDRs in the whitelist, including CIDRs with host bits" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "2001:db8:abcd::1234/48" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("2001:db8:abcd:ffff::1", None),
          RemoteInfo.ip("2001:db8:abce::1", None)
        ) must_== Seq(OK, FORBIDDEN)
      }
    }

    "keep an IPv4 /0 separate from IPv6" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "0.0.0.0/0" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("203.0.113.250", None),
          RemoteInfo.ip("2001:db8::1", None),
          RemoteInfo(RemoteNode.Unknown(None), None)
        ) must_== Seq(OK, FORBIDDEN, FORBIDDEN)
      }
    }

    "keep an IPv6 /0 separate from IPv4" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "::/0" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("2001:db8::1", None),
          RemoteInfo.ip("203.0.113.250", None),
          RemoteInfo(RemoteNode.Unknown(None), None)
        ) must_== Seq(OK, FORBIDDEN, FORBIDDEN)
      }
    }

    "match both sides of an IPv4 /25 boundary" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "192.0.2.128/25" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("192.0.2.128", None),
          RemoteInfo.ip("192.0.2.255", None),
          RemoteInfo.ip("192.0.2.127", None),
          RemoteInfo.ip("192.0.3.0", None)
        ) must_== Seq(OK, OK, FORBIDDEN, FORBIDDEN)
      }
    }

    "treat an IPv4 /32 as an exact, family-specific match" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "192.0.2.7/32" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("192.0.2.7", None),
          RemoteInfo.ip("192.0.2.8", None),
          RemoteInfo.ip("2001:db8::c000:207", None)
        ) must_== Seq(OK, FORBIDDEN, FORBIDDEN)
      }
    }

    "match both sides of an IPv6 /65 boundary" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "2001:db8:0:0:8000::/65" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("2001:db8:0:0:8000::", None),
          RemoteInfo.ip("2001:db8:0:0:ffff:ffff:ffff:ffff", None),
          RemoteInfo.ip("2001:db8:0:0:7fff:ffff:ffff:ffff", None),
          RemoteInfo.ip("2001:db8:0:1:8000::", None)
        ) must_== Seq(OK, OK, FORBIDDEN, FORBIDDEN)
      }
    }

    "treat an IPv6 /128 as an exact, family-specific match" in new WithApplication(
      buildApp("""
                 |play.filters.ip.whiteList = [ "2001:db8::7/128" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("2001:db8::7", None),
          RemoteInfo.ip("2001:db8::8", None),
          RemoteInfo.ip("192.0.2.7", None)
        ) must_== Seq(OK, FORBIDDEN, FORBIDDEN)
      }
    }

    "apply IPv4 and IPv6 CIDRs in the blacklist" in new WithApplication(
      buildApp("""
                 |play.filters.ip.blackList = [ "192.0.2.0/24", "2001:db8::/32" ]
      """.stripMargin)
    ) with Injecting {
      override def running() = {
        remoteStatuses(
          app,
          RemoteInfo.ip("192.0.2.99", None),
          RemoteInfo.ip("198.51.100.99", None),
          RemoteInfo.ip("2001:db8:ffff::1", None),
          RemoteInfo.ip("2001:db9::1", None)
        ) must_== Seq(FORBIDDEN, OK, FORBIDDEN, OK)
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

  "IPFilter configuration validation" should {
    "accept all supported identity and network forms at startup" in {
      val validEntries = Seq(
        "192.0.2.1",
        "2001:db8::1",
        "192.0.2.123/24",
        "2001:db8::123/64",
        "unknown",
        "UNKNOWN",
        "_edge-1.v2"
      )

      configurationLoadResults(validEntries) must_== Seq.fill(validEntries.size * 2)(true)
    }

    "reject hostnames and malformed identity tokens at startup" in {
      val invalidEntries = Seq(
        "localhost",
        "example.com",
        "edge",
        "_",
        "unknown.example",
        "_edge!"
      )

      configurationLoadResults(invalidEntries) must_== Seq.fill(invalidEntries.size * 2)(false)
    }

    "reject legacy IPv4 shorthand, integer, and leading-zero forms in both lists at startup" in {
      val invalidEntries = Seq(
        "127.1",
        "192.168.1",
        "2130706433",
        "001.002.003.004"
      )

      configurationLoadResults(invalidEntries) must_== Seq.fill(invalidEntries.size * 2)(false)
    }

    "reject non-ASCII lookalikes for numeric IPs and unknown at startup" in {
      val invalidEntries = Seq(
        "１９２.０.２.１",
        "unKnown"
      )

      configurationLoadResults(invalidEntries) must_== Seq.fill(invalidEntries.size * 2)(false)
    }

    "reject malformed CIDRs and invalid prefixes at startup" in {
      val invalidEntries = Seq(
        "192.0.2.1/",
        "/24",
        "192.0.2.1/24/1",
        "192.0.2.1/prefix",
        "192.0.2.1/-1",
        "192.0.2.1/33",
        "192.0.2.1/999999999999999999999999999999",
        "2001:db8::1/129",
        "192.0.2.999/24",
        "2001:db8::gg/64",
        "fe80::1%0",
        "unknown/0"
      )

      configurationLoadResults(invalidEntries) must_== Seq.fill(invalidEntries.size * 2)(false)
    }

    "reject whitespace, bracketed literals, and endpoint ports at startup" in {
      val invalidEntries = Seq(
        "",
        " 192.0.2.1",
        "192.0.2.1 ",
        "\t_edge",
        "192.0.2.0 /24",
        "192.0.2.1:443",
        "[2001:db8::1]",
        "[2001:db8::1]:443",
        "_edge:443",
        "unknown:443"
      )

      configurationLoadResults(invalidEntries) must_== Seq.fill(invalidEntries.size * 2)(false)
    }
  }

  private def request(path: String, ip: String): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest(method = "GET", path = path)
      .withRemote(RemoteInfo.ip(ip, None))
      .withAuthority(Some(RequestAuthority.parseOrThrow("playframework.com")))
  }

  private def request(
      path: String,
      remoteNode: RemoteNode
  ): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest(method = "GET", path = path)
      .withRemote(RemoteInfo(remoteNode, None))
      .withAuthority(Some(RequestAuthority.parseOrThrow("playframework.com")))
  }

  private def request(
      path: String,
      remoteInfo: RemoteInfo
  ): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest(method = "GET", path = path)
      .withRemote(remoteInfo)
      .withAuthority(Some(RequestAuthority.parseOrThrow("playframework.com")))
  }

  private def remoteStatuses(app: Application, remotes: RemoteInfo*): Seq[Int] =
    remotes.map(remote => status(route(app, request("/", remote)).get))

  private def requestWithRouteModifiers(
      path: String,
      remoteInfo: RemoteInfo,
      modifiers: Seq[String]
  ): Request[AnyContentAsEmpty.type] =
    request(path, remoteInfo).addAttr(
      Router.Attrs.HandlerDef,
      HandlerDef(
        getClass.getClassLoader,
        "routes",
        "FooController",
        "foo",
        Seq.empty,
        "GET",
        path,
        "comments",
        modifiers
      )
    )

  private val baseIpFilterConfiguration = ConfigFactory.parseString("""
                                                                      |play.filters.ip {
                                                                      |  accessDeniedHttpStatusCode = 403
                                                                      |  whiteList = []
                                                                      |  blackList = []
                                                                      |  routeModifiers {
                                                                      |    whiteList = ["anyip"]
                                                                      |    blackList = []
                                                                      |  }
                                                                      |}
                                                                      |""".stripMargin)

  private def configurationLoadResults(entries: Seq[String]): Seq[Boolean] =
    for {
      listName <- Seq("whiteList", "blackList")
      entry    <- entries
    } yield Try {
      val entryList = ConfigValueFactory.fromIterable(java.util.Collections.singletonList(entry))
      val config    = baseIpFilterConfiguration.withValue(s"play.filters.ip.$listName", entryList)
      new IPFilterConfigProvider(Configuration(config)).get
    }.isSuccess

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
