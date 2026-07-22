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

class ForwardedHeaderSchemeSpec extends Specification with ForwardedHeaderHandlerSpecSupport {
  "ForwardedHeaderHandler" should {
    "use rfc7239 proto without for from a directly trusted proxy" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: proto=https
        """.stripMargin
      ) mustEqual expectedResult(localhost, true)
    }

    "ignore invalid rfc7239 proto without for and retain the effective scheme" in {
      forwardedResultFrom(
        handler(version("rfc7239") ++ trustedProxies("127.0.0.1")),
        expectedResult("127.0.0.1", true),
        headers("""
                  |Forwarded: proto=1https
        """.stripMargin)
      ) mustEqual expectedResult(localhost, true)
    }

    "stop scanning before entries preceding rfc7239 proto without for" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;proto=http, proto=https
        """.stripMargin
      ) mustEqual expectedResult(localhost, true)
    }

    "get first untrusted proxy protocol with rfc7239 with trusted localhost proxy" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.10", false)
    }

    "get first untrusted proxy protocol with rfc7239 with subnet mask" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=https;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult(
        RemoteInfo(
          RemoteNode.Ip(addr("192.0.2.60"), None),
          Some(RemoteNode.Ip(addr("203.0.113.43"), None))
        ),
        true
      )
    }

    "treat rfc7239 proto schemes case-insensitively" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;proto=HtTpS
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "retain a valid custom rfc7239 proto as an insecure effective scheme" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;proto=webcal
        """.stripMargin
      )

      forwarding.scheme mustEqual Scheme.parseOrThrow("webcal")
      forwarding.remote.identity mustEqual "203.0.113.43"
    }

    "retain the previously verified scheme when the x-forwarded proto list is missing" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "not downgrade an existing HTTPS scheme when the x-forwarded proto list is missing" in {
      forwardedResultFrom(
        handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")),
        expectedResult(localhost, true),
        headers("""
                  |X-Forwarded-For: 203.0.113.43
        """.stripMargin)
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "treat x-forwarded-proto schemes case-insensitively" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: HtTpS
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "retain the previously verified scheme when the x-forwarded proto list is shorter than the for list" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "retain the previously verified scheme when the proto list is shorter and all addresses are trusted" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "associate a single x-forwarded-proto with the client when trustSingleXForwardedProto is enabled" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "associate a single x-forwarded-proto with the client when trustSingleXForwardedProto is enabled and all addresses are trusted" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "associate a single x-forwarded-proto with the client when multiple forwarded-for entries are trusted" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43, 192.168.1.44
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "stop at an untrusted proxy when trustSingleXForwardedProto is enabled" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 198.51.100.17, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual expectedResult("198.51.100.17", false)
    }

    "retain the previously verified scheme for a shorter multi-value proto list even with single-proto trust" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43, 192.168.1.44
          |X-Forwarded-Proto: https, https
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "preserve the last verified rfc7239 scheme when an earlier entry omits proto" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1") ++ trustSingleXForwardedProto(true),
        """
          |Forwarded: for=203.0.113.43
          |Forwarded: for=192.168.1.43;proto=https
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "ignore single x-forwarded-proto when x-forwarded-for is missing even when trustSingleXForwardedProto is enabled" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "use single x-forwarded-proto without x-forwarded-for when enabled and proxy is trusted" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual expectedResult(localhost, true)
    }

    "use single x-forwarded-proto without x-forwarded-for case-insensitively" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |X-Forwarded-Proto: HTTPS
        """.stripMargin
      ) mustEqual expectedResult(localhost, true)
    }

    "ignore single x-forwarded-proto without x-forwarded-for when proxy is untrusted" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.0.2.1") ++ trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "ignore multiple x-forwarded-proto values without x-forwarded-for even when enabled" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |X-Forwarded-Proto: https, http
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "ignore x-forwarded-ssl by default" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "use x-forwarded-ssl without x-forwarded-for when enabled and proxy is trusted" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult(localhost, true)
    }

    "interpret x-forwarded-ssl case-insensitively" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-Ssl: ON
        """.stripMargin
      ) mustEqual expectedResult(localhost, true)
    }

    "interpret x-forwarded-ssl off as an insecure original request" in {
      val result = forwardedResultFrom(
        handler(version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true)),
        expectedResult("127.0.0.1", true),
        headers("X-Forwarded-Ssl: off")
      )

      result mustEqual expectedResult(localhost, false)
    }

    "ignore an unrecognized x-forwarded-ssl value" in {
      val result = forwardedResultFrom(
        handler(version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true)),
        expectedResult("127.0.0.1", true),
        headers("X-Forwarded-Ssl: yes")
      )

      result mustEqual expectedResult(localhost, true)
    }

    "ignore comma-separated x-forwarded-ssl values" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-Ssl: on, off
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "ignore repeated x-forwarded-ssl values" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-Ssl: on
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "ignore x-forwarded-ssl from an untrusted directly connected proxy" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.0.2.1") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "associate x-forwarded-ssl with the client x-forwarded-for entry" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43, 192.168.1.44
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "stop before x-forwarded-ssl at an untrusted intermediary" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-For: 203.0.113.43, 198.51.100.17, 192.168.1.43
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult("198.51.100.17", false)
    }

    "prefer an insecure x-forwarded-proto over an enabled x-forwarded-ssl value" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: http
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "prefer a secure x-forwarded-proto over an enabled x-forwarded-ssl value" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: https
          |X-Forwarded-Ssl: off
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "not fall back to x-forwarded-ssl when x-forwarded-proto is ambiguous" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43, 192.168.1.44
          |X-Forwarded-Proto: https, https
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "not fall back to x-forwarded-ssl when x-forwarded-proto without x-forwarded-for is not trusted" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true),
        """
          |X-Forwarded-Proto: https
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "not apply trustXForwardedSsl to rfc7239 forwarded headers" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustXForwardedSsl(true),
        """
          |Forwarded: for=203.0.113.43
          |X-Forwarded-Ssl: on
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "retain the previously verified scheme when the x-forwarded proto list is longer than the for list" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, https, https
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "retain a valid custom x-forwarded proto as the effective scheme" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: smtp
        """.stripMargin
      ) mustEqual ForwardedResult(RemoteInfo.ip("203.0.113.43", None), Scheme.parseOrThrow("smtp"))
    }

    "handle lots of different IPv6 address and proto quoting in x-forwarded-for headers" in {
      forwardedResultToLocalhost(
        version("x-forwarded"),
        """
          |X-Forwarded-For: 192.0.2.43, "::1", ::1, "[::1]", [::1]
          |X-Forwarded-Proto: "https", http, http,    "http", http
        """.stripMargin
      ) mustEqual expectedResult("192.0.2.43", true)
    }

  }
}
