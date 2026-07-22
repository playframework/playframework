/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.InetAddress

import com.google.common.net.InetAddresses
import org.specs2.mutable.Specification
import play.api.mvc.request.NodePort
import play.api.mvc.request.RemoteInfo
import play.api.mvc.request.RemoteNode
import play.api.mvc.request.RequestAuthority
import play.api.mvc.request.Scheme
import play.api.mvc.Headers
import play.api.Configuration
import play.api.PlayException
import play.core.server.common.ForwardedHeaderHandler._

class ForwardedHeaderAuthoritySpec extends Specification with ForwardedHeaderHandlerSpecSupport {
  "ForwardedHeaderHandler" should {
    "use rfc7239 host from the selected trusted forwarded entry" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++
          trustedProxies("127.0.0.1", "192.168.1.1/24") ++
          trustForwardedHost(true),
        """
          |Host: internal.example
          |Forwarded: for=203.0.113.43;host="public.example:9443"
          |Forwarded: for=192.168.1.43;host=proxy.example
        """.stripMargin
      )

      forwarding.authority.map(_.render) must beSome("public.example:9443")
      forwarding.remote.identity mustEqual "203.0.113.43"
    }

    "not use rfc7239 host by default" in {
      forwardedHostToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;host=public.example
        """.stripMargin
      ) must beNone
    }

    "ignore rfc7239 host when the forwarding proxy is not trusted" in {
      forwardedHostToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24") ++ trustForwardedHost(true),
        """
          |Host: internal.example
          |Forwarded: for=203.0.113.43;host=public.example
        """.stripMargin
      ) must beNone
    }

    "preserve the last verified rfc7239 authority when an earlier entry omits host" in {
      forwardedHostToLocalhost(
        version("rfc7239") ++
          trustedProxies("127.0.0.1", "192.168.1.1/24") ++
          trustForwardedHost(true),
        """
          |Host: internal.example
          |Forwarded: for=203.0.113.43
          |Forwarded: for=192.168.1.43;host=proxy.example
        """.stripMargin
      ) must beSome("proxy.example")
    }

    "ignore empty-host rfc7239 authorities without discarding other metadata" in {
      val forwarding = Seq("", ":8080").map { host =>
        forwardedRequestToLocalhost(
          version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
          s"""Forwarded: for=203.0.113.43;proto=https;host="$host"""",
          "internal.example:9000"
        )
      }

      forwarding.map(_.authority.map(_.render)) mustEqual Seq.fill(2)(Some("internal.example:9000"))
      forwarding.map(_.remote.identity) mustEqual Seq.fill(2)("203.0.113.43")
      forwarding.map(_.scheme) mustEqual Seq.fill(2)(Scheme.Https)
    }

    "not reject an empty-host rfc7239 authority when host forwarding is disabled" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """Forwarded: for=203.0.113.43;proto=https;host=":8080"""",
        "internal.example:9000"
      )

      forwarding.authority.map(_.render) must beSome("internal.example:9000")
      forwarding.remote.identity mustEqual "203.0.113.43"
      forwarding.scheme mustEqual Scheme.Https
    }

    "use rfc7239 host without for from a directly trusted proxy" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
        """
          |Forwarded: host=public.example;proto=https
        """.stripMargin
      )

      forwarding.authority.map(_.render) must beSome("public.example")
      forwarding.remote.identity mustEqual "127.0.0.1"
      forwarding.scheme mustEqual Scheme.Https
    }

    "stop scanning before entries preceding an rfc7239 host without for" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
        """
          |Forwarded: for=203.0.113.43;host=client.example, host=proxy.example
        """.stripMargin
      )

      forwarding.authority.map(_.render) must beSome("proxy.example")
      forwarding.remote.identity mustEqual "127.0.0.1"
    }

    "not apply trustForwardedHost to x-forwarded headers" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
        """
          |Forwarded: for=203.0.113.43;host=public.example
          |X-Forwarded-For: 203.0.113.43
        """.stripMargin
      ) must beNone
    }

    "not use x-forwarded-host as the effective host by default" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        "X-Forwarded-Host: public.example:8443",
        "internal.example:9000"
      ) must beNone
    }

    "use x-forwarded-host as the effective host when enabled" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true),
        "X-Forwarded-Host: public.example:8443",
        "internal.example:9000"
      ) must beSome("public.example:8443")
    }

    "use x-forwarded-host without x-forwarded-for" in {
      val forwarding = handler(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true)
      ).forwardedRequest(
        expectedResult("127.0.0.1", Some(53124), false).remote,
        headers("X-Forwarded-Host: public.example"),
        Scheme.Http,
        Some(RequestAuthority.parseOrThrow("internal.example"))
      )

      forwarding.authority.map(_.render) must beSome("public.example")
      forwardedResult(forwarding) mustEqual expectedResult(localhost, Some(53124), false)
    }

    "ignore x-forwarded-host from an untrusted direct peer" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.0.2.1") ++ trustXForwardedHost(true),
        "X-Forwarded-Host: public.example",
        "internal.example"
      ) must beNone
    }

    "ignore missing and malformed x-forwarded-host values" in {
      val config         = version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true)
      val invalidHeaders = Seq(
        "",
        "X-Forwarded-Host:",
        "X-Forwarded-Host: user@example.org",
        "X-Forwarded-Host: example.org/path",
        "X-Forwarded-Host: 2001:db8::1",
        "X-Forwarded-Host: \"public.example\""
      )

      invalidHeaders.forall { headerText =>
        forwardedHostToLocalhost(config, headerText, "internal.example").isEmpty
      } must beTrue
    }

    "ignore a port-only x-forwarded-host without discarding other metadata" in {
      val forwarding = forwardedRequestToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: https
          |X-Forwarded-Host: :8080
        """.stripMargin,
        "internal.example:9000"
      )

      forwarding.authority.map(_.render) must beSome("internal.example:9000")
      forwarding.remote.identity mustEqual "203.0.113.43"
      forwarding.scheme mustEqual Scheme.Https
    }

    "apply an independently trusted x-forwarded-port when x-forwarded-host is port-only" in {
      val forwarding = forwardedRequestToLocalhost(
        version("x-forwarded") ++
          trustedProxies("127.0.0.1") ++
          trustXForwardedHost(true) ++
          trustXForwardedPort(true),
        """
          |X-Forwarded-Host: :8080
          |X-Forwarded-Port: 7000
        """.stripMargin,
        "internal.example:9000"
      )

      forwarding.authority.map(_.render) must beSome("internal.example:7000")
    }

    "ignore multiple x-forwarded-host values" in {
      val config = version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true)

      forwardedHostToLocalhost(
        config,
        "X-Forwarded-Host: client.example, public.example",
        "internal.example"
      ) must beNone
      forwardedHostToLocalhost(
        config,
        """
          |X-Forwarded-Host: client.example
          |X-Forwarded-Host: public.example
        """.stripMargin,
        "internal.example"
      ) must beNone
    }

    "apply x-forwarded-host to bracketed IPv6 authorities" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true),
        "X-Forwarded-Host: [2001:db8::1]:8443",
        "internal.example"
      ) must beSome("[2001:db8::1]:8443")
    }

    "apply both forwarded-host families to IPv4-mapped IPv6 authorities" in {
      forwardedHostToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
        """Forwarded: for=203.0.113.43;host="[::ffff:192.0.2.43]:8443"""",
        "internal.example"
      ) must beSome("[::ffff:c000:22b]:8443")
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true),
        "X-Forwarded-Host: [::ffff:192.0.2.43]:8443",
        "internal.example"
      ) must beSome("[::ffff:c000:22b]:8443")
    }

    "reject scoped and non-ASCII values from both forwarded-host families" in {
      val invalid = Seq("[fe80::1%1]", "[fe80::1%eth0]", "[fe80::1%25eth0]", "１２７.０.０.１", "١٢٧.٠.٠.١")

      invalid.forall { value =>
        forwardedHostToLocalhost(
          version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
          s"""Forwarded: for=203.0.113.43;host="$value"""",
          "internal.example"
        ).isEmpty
      } must beTrue
      invalid.forall { value =>
        forwardedHostToLocalhost(
          version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true),
          s"X-Forwarded-Host: $value",
          "internal.example"
        ).isEmpty
      } must beTrue
    }

    "keep x-forwarded-for source ports separate from x-forwarded-host ports" in {
      val forwarding = forwardedRequestToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true),
        """
          |X-Forwarded-For: 203.0.113.43:53124
          |X-Forwarded-Host: public.example:8443
        """.stripMargin,
        "internal.example"
      )

      forwarding.authority.map(_.render) must beSome("public.example:8443")
      forwardedResult(forwarding) mustEqual expectedResult("203.0.113.43", Some(53124), false)
    }

    "not apply trustXForwardedHost to rfc7239 forwarded headers" in {
      forwardedHostToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustXForwardedHost(true),
        """
          |Forwarded: for=203.0.113.43
          |X-Forwarded-Host: public.example
        """.stripMargin,
        "internal.example"
      ) must beNone
    }

    "not use x-forwarded-port as the effective host port by default" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-Port: 8443
        """.stripMargin,
        "public.example:9000"
      ) must beNone
    }

    "use x-forwarded-port as the effective host port when enabled" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedPort(true),
        """
          |X-Forwarded-Port: 8443
        """.stripMargin,
        "public.example:9000"
      ) must beSome("public.example:8443")
    }

    "accept arbitrary decimal x-forwarded-port values" in {
      val config = version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedPort(true)

      forwardedHostToLocalhost(config, "X-Forwarded-Port: 0", "public.example") must beSome("public.example:0")
      forwardedHostToLocalhost(config, "X-Forwarded-Port: 65535", "public.example") must beSome(
        "public.example:65535"
      )
      forwardedHostToLocalhost(config, "X-Forwarded-Port: 12345678901234567890", "public.example") must beSome(
        "public.example:12345678901234567890"
      )
    }

    "retain the initial authority when host forwarding is disabled" in {
      val forwarding = handler(version("x-forwarded")).forwardedRequest(
        expectedResult("127.0.0.1", false).remote,
        Headers(),
        Scheme.Http,
        Some(RequestAuthority.parseOrThrow("public.example"))
      )

      forwarding.remote.ipAddress must beSome(localhost)
      forwarding.authority must beSome(RequestAuthority.parseOrThrow("public.example"))
    }

    "use x-forwarded-port without x-forwarded-for" in {
      val forwarding = forwardedRequestToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedPort(true),
        """
          |X-Forwarded-Port: 8443
        """.stripMargin,
        "public.example"
      )

      forwarding.authority.map(_.render) must beSome("public.example:8443")
      forwardedResult(forwarding) mustEqual expectedResult(localhost, false)
    }

    "ignore x-forwarded-port from an untrusted direct peer" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.0.2.1") ++ trustXForwardedPort(true),
        """
          |X-Forwarded-Port: 8443
        """.stripMargin,
        "public.example"
      ) must beNone
    }

    "ignore missing and malformed x-forwarded-port values" in {
      val config         = version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedPort(true)
      val invalidHeaders = Seq(
        "",
        "X-Forwarded-Port: http",
        "X-Forwarded-Port: -1",
        "X-Forwarded-Port: \"443\""
      )

      invalidHeaders.forall { headerText =>
        forwardedHostToLocalhost(config, headerText, "public.example").isEmpty
      } must beTrue
    }

    "ignore multiple x-forwarded-port values" in {
      val config = version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedPort(true)

      forwardedHostToLocalhost(config, "X-Forwarded-Port: 443, 8443", "public.example") must beNone
      forwardedHostToLocalhost(
        config,
        """
          |X-Forwarded-Port: 443
          |X-Forwarded-Port: 8443
        """.stripMargin,
        "public.example"
      ) must beNone
    }

    "apply x-forwarded-port to bracketed IPv6 hosts" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedPort(true),
        "X-Forwarded-Port: 8443",
        "[2001:db8::1]:9000"
      ) must beSome("[2001:db8::1]:8443")
    }

    "combine trusted x-forwarded-port and proto without changing the remote source port" in {
      val forwarding = forwardedRequestToLocalhost(
        version("x-forwarded") ++
          trustedProxies("127.0.0.1") ++
          trustXForwardedPort(true) ++
          trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |X-Forwarded-Port: 8443
          |X-Forwarded-Proto: https
        """.stripMargin,
        "public.example"
      )

      forwarding.authority.map(_.render) must beSome("public.example:8443")
      forwardedResult(forwarding) mustEqual expectedResult(localhost, true)
    }

    "let x-forwarded-port replace the port in x-forwarded-host" in {
      forwardedHostToLocalhost(
        version("x-forwarded") ++
          trustedProxies("127.0.0.1") ++
          trustXForwardedHost(true) ++
          trustXForwardedPort(true),
        """
          |X-Forwarded-Host: public.example:9000
          |X-Forwarded-Port: 8443
        """.stripMargin,
        "internal.example:7000"
      ) must beSome("public.example:8443")
    }

    "keep independently valid x-forwarded authority metadata" in {
      val config = version("x-forwarded") ++
        trustedProxies("127.0.0.1") ++
        trustXForwardedHost(true) ++
        trustXForwardedPort(true)

      forwardedHostToLocalhost(
        config,
        """
          |X-Forwarded-Host: public.example:9000
          |X-Forwarded-Port: invalid
        """.stripMargin,
        "internal.example:7000"
      ) must beSome("public.example:9000")
      forwardedHostToLocalhost(
        config,
        """
          |X-Forwarded-Host: user@example.org
          |X-Forwarded-Port: 8443
        """.stripMargin,
        "internal.example:7000"
      ) must beSome("internal.example:8443")
    }

    "keep x-forwarded-for source ports separate from the effective host port" in {
      val forwarding = forwardedRequestToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedPort(true),
        """
          |X-Forwarded-For: 203.0.113.43:53124
          |X-Forwarded-Proto: https
          |X-Forwarded-Port: 8443
        """.stripMargin,
        "public.example:9000"
      )

      forwarding.authority.map(_.render) must beSome("public.example:8443")
      forwardedResult(forwarding) mustEqual expectedResult("203.0.113.43", Some(53124), true)
    }

    "not apply trustXForwardedPort to rfc7239 forwarded headers" in {
      forwardedHostToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustXForwardedPort(true),
        """
          |Forwarded: for=203.0.113.43
          |X-Forwarded-Port: 8443
        """.stripMargin,
        "public.example"
      ) must beNone
    }

    "not apply trustXForwardedProtoWithoutXForwardedFor while forwarding an rfc7239 host" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++
          trustedProxies("127.0.0.1") ++
          trustForwardedHost(true) ++
          trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |Forwarded: host=public.example
          |X-Forwarded-Proto: HTTPS
        """.stripMargin
      )

      forwardedResult(forwarding) mustEqual expectedResult(localhost, false)
      forwarding.authority.map(_.render) must beSome("public.example")
    }

  }
}
