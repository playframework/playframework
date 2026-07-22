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

class ForwardedHeaderNodeMetadataSpec extends Specification with ForwardedHeaderHandlerSpecSupport {
  "ForwardedHeaderHandler" should {
    "parse rfc7239 entries" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies("192.0.2.60/24"),
        headers(
          """
            |Forwarded: for="_gazonk"
            |Forwarded: For="[2001:db8:cafe::17]:4711"
            |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
            |Forwarded: for=192.0.2.43, for=198.51.100.17, for=127.0.0.1
            |Forwarded: for=192.0.2.61;proto=https
            |Forwarded: for=unknown
            |Forwarded: For="[::ffff:192.168.0.9]";proto=https
          """.stripMargin
        )
      )
      results.length must_== 9
      results(0)._1 must_== ForwardedEntry(Some("_gazonk"), None)
      results(0)._2 must beRight(ParsedForwardedEntry(RemoteInfo(RemoteNode.Obfuscated("_gazonk", None), None)))
      results(0)._3 must beSome(false)
      results(0)._4 must beNone
      results(1)._1 must_== ForwardedEntry(Some("[2001:db8:cafe::17]:4711"), None)
      results(1)._2 must beRight(
        ParsedForwardedEntry(
          RemoteInfo(RemoteNode.Ip(addr("2001:db8:cafe::17"), Some(NodePort.Numeric(4711))), None)
        )
      )
      results(1)._3 must beSome(false)
      results(1)._4 must beNone
      results(2)._1 must_== ForwardedEntry(Some("192.0.2.60"), Some("http"), byString = Some("203.0.113.43"))
      results(2)._2 must beRight(
        ParsedForwardedEntry(
          RemoteInfo(
            RemoteNode.Ip(addr("192.0.2.60"), None),
            Some(RemoteNode.Ip(addr("203.0.113.43"), None))
          )
        )
      )
      results(2)._3 must beSome(true)
      results(2)._4 must beSome(Scheme.Http)
      results(3)._1 must_== ForwardedEntry(Some("192.0.2.43"), None)
      results(3)._2 must beRight(ParsedForwardedEntry(RemoteInfo.ip(addr("192.0.2.43"), None)))
      results(3)._3 must beSome(true)
      results(4)._1 must_== ForwardedEntry(Some("198.51.100.17"), None)
      results(4)._2 must beRight(ParsedForwardedEntry(RemoteInfo.ip(addr("198.51.100.17"), None)))
      results(4)._3 must beSome(false)
      results(5)._1 must_== ForwardedEntry(Some("127.0.0.1"), None)
      results(5)._2 must beRight(ParsedForwardedEntry(RemoteInfo.ip(addr("127.0.0.1"), None)))
      results(5)._3 must beSome(false)
      results(6)._1 must_== ForwardedEntry(Some("192.0.2.61"), Some("https"))
      results(6)._2 must beRight(ParsedForwardedEntry(RemoteInfo.ip(addr("192.0.2.61"), None)))
      results(6)._3 must beSome(true)
      results(6)._4 must beSome(Scheme.Https)
      results(7)._1 must_== ForwardedEntry(Some("unknown"), None)
      results(7)._2 must beRight(ParsedForwardedEntry(RemoteInfo(RemoteNode.Unknown(None), None)))
      results(7)._3 must beSome(false)
      results(8)._1 must_== ForwardedEntry(Some("[::ffff:192.168.0.9]"), Some("https"))
      results(8)._2 must beRight(
        ParsedForwardedEntry(RemoteInfo.ip(addr("::ffff:192.168.0.9"), None))
      )
      results(8)._3 must beSome(false)
      results(8)._4 must beSome(Scheme.Https)
    }

    "parse valid rfc7239 by entries and reject invalid ones without changing selected remote identity" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies(),
        headers(
          """
            |Forwarded: for=192.0.2.43;proto=https;by=203.0.113.43
            |Forwarded: for=192.0.2.44;by="_edge"
            |Forwarded: for=192.0.2.45;by=unknown
            |Forwarded: for=192.0.2.46;by="[2001:db8:cafe::17]:4711"
            |Forwarded: for=192.0.2.47;by="???"
          """.stripMargin
        )
      )

      results(0)._2 must beRight(
        ParsedForwardedEntry(
          RemoteInfo(
            RemoteNode.Ip(addr("192.0.2.43"), None),
            Some(RemoteNode.Ip(addr("203.0.113.43"), None))
          )
        )
      )
      results(1)._2 must beRight(
        ParsedForwardedEntry(
          RemoteInfo(
            RemoteNode.Ip(addr("192.0.2.44"), None),
            Some(RemoteNode.Obfuscated("_edge", None))
          )
        )
      )
      results(2)._2 must beRight(
        ParsedForwardedEntry(
          RemoteInfo(RemoteNode.Ip(addr("192.0.2.45"), None), Some(RemoteNode.Unknown(None)))
        )
      )
      results(3)._2 must beRight(
        ParsedForwardedEntry(
          RemoteInfo(
            RemoteNode.Ip(addr("192.0.2.46"), None),
            Some(RemoteNode.Ip(addr("2001:db8:cafe::17"), Some(NodePort.Numeric(4711))))
          )
        )
      )
      results(4)._2 must beLeft
      results(4)._3 must beNone
    }

    "parse rfc7239 quoted-pair escapes in forwarded nodes" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies(),
        headers(
          """
            |Forwarded: for="_edge\.1";by="_proxy\-1"
          """.stripMargin
        )
      )

      results(0)._1 must_== ForwardedEntry(Some("_edge.1"), None, byString = Some("_proxy-1"))
      results(0)._2 must beRight(
        ParsedForwardedEntry(
          RemoteInfo(
            RemoteNode.Obfuscated("_edge.1", None),
            Some(RemoteNode.Obfuscated("_proxy-1", None))
          )
        )
      )
    }

    "expose rfc7239 by entries on the selected remote" in {
      val result = forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;by=192.0.2.10;proto=https
        """.stripMargin
      )

      result.remote.node must beEqualTo(RemoteNode.Ip(addr("203.0.113.43"), None))
      result.remote.byNode must beSome(RemoteNode.Ip(addr("192.0.2.10"), None))
      result.scheme mustEqual Scheme.Https
    }

    "parse x-forwarded entries" in {
      val results = processHeaders(
        version("x-forwarded") ++ trustedProxies("2001:db8:cafe::17"),
        headers(
          """
            |X-Forwarded-For: 192.168.1.1, ::1, [2001:db8:cafe::17], 127.0.0.1, ::ffff:123.123.123.123
            |X-Forwarded-Proto: https, http, https, http, https
        """.stripMargin
        )
      )
      results.length must_== 5
      results(0)._1 must_== ForwardedEntry(Some("192.168.1.1"), Some("https"))
      results(0)._2 must beRight(ParsedForwardedEntry(RemoteInfo.ip(addr("192.168.1.1"), None)))
      results(0)._3 must beSome(false)
      results(0)._4 must beSome(Scheme.Https)
      results(1)._1 must_== ForwardedEntry(Some("::1"), Some("http"))
      results(1)._2 must beRight(ParsedForwardedEntry(RemoteInfo.ip(addr("::1"), None)))
      results(1)._3 must beSome(false)
      results(1)._4 must beSome(Scheme.Http)
      results(2)._1 must_== ForwardedEntry(Some("[2001:db8:cafe::17]"), Some("https"))
      results(2)._2 must beRight(ParsedForwardedEntry(RemoteInfo.ip(addr("2001:db8:cafe::17"), None)))
      results(2)._3 must beSome(true)
      results(2)._4 must beSome(Scheme.Https)
      results(3)._1 must_== ForwardedEntry(Some("127.0.0.1"), Some("http"))
      results(3)._2 must beRight(ParsedForwardedEntry(RemoteInfo.ip(addr("127.0.0.1"), None)))
      results(3)._3 must beSome(false)
      results(3)._4 must beSome(Scheme.Http)
      results(4)._1 must_== ForwardedEntry(Some("::ffff:123.123.123.123"), Some("https"))
      results(4)._2 must beRight(
        ParsedForwardedEntry(RemoteInfo.ip(addr("::ffff:123.123.123.123"), None))
      )
      results(4)._3 must beSome(false)
      results(4)._4 must beSome(Scheme.Https)
    }

    "default to trusting IPv4 and IPv6 localhost with rfc7239 when there is config with default settings" in {
      forwardedResultToLocalhost(
        version("rfc7239"),
        """
          |Forwarded: for=192.0.2.43;proto=https, for="[::1]"
        """.stripMargin
      ) mustEqual expectedResult("192.0.2.43", true)
    }

    "ignore proxy hosts with rfc7239 when no proxies are trusted" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies(),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=198.51.100.17, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "get first untrusted proxy host with rfc7239 with ipv4 localhost" in {
      forwardedResultToLocalhost(
        version("rfc7239"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=198.51.100.17, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("198.51.100.17", false)
    }

    "get first untrusted proxy host with rfc7239 with ipv6 localhost" in {
      forwardedResultToLocalhost(
        version("rfc7239"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=[::1]
        """.stripMargin
      ) mustEqual expectedResult("192.0.2.43", false)
    }

    "get first untrusted proxy with rfc7239 with trusted proxy subnet" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult(
        RemoteInfo(
          RemoteNode.Ip(addr("192.0.2.60"), None),
          Some(RemoteNode.Ip(addr("203.0.113.43"), None))
        ),
        false
      )
    }

    "handle unquoted IPv6 addresses with rfc7239 for compatibility" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: For=[2001:db8:cafe::17]:4711
        """.stripMargin
      ) mustEqual expectedResult("2001:db8:cafe::17", Some(4711), false)
    }

    "handle quoted IPv6 addresses with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: For="[2001:db8:cafe::17]:4711"
        """.stripMargin
      ) mustEqual expectedResult("2001:db8:cafe::17", Some(4711), false)
    }

    "handle quoted IPv4-mapped IPv6 addresses with rfc7239" in {
      forwardedResultFrom(
        handler(version("rfc7239") ++ trustedProxies("fe80::1", "::ffff:123.123.123.123")),
        expectedResult("fe80::1", false),
        headers("""
                  |Forwarded: For="[::ffff:99.99.99.99]:4711"
                  |Forwarded: For="[::ffff:123.123.123.123]"
        """.stripMargin)
      ) mustEqual expectedResult(addr("::ffff:99.99.99.99"), Some(4711), false)
    }

    "use obfuscated addresses with rfc7239 without inventing an IP address" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult(RemoteInfo(RemoteNode.Obfuscated("_gazonk", None), None), false)
    }

    "use proto from obfuscated addresses with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="_gazonk";proto=https
        """.stripMargin
      ) mustEqual expectedResult(RemoteInfo(RemoteNode.Obfuscated("_gazonk", None), None), true)
    }

    "stop scanning at obfuscated addresses with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;proto=https
          |Forwarded: for="_gazonk";proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual expectedResult(RemoteInfo(RemoteNode.Obfuscated("_gazonk", None), None), false)
    }

    "stop scanning at unknown identifiers with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;proto=https
          |Forwarded: for=unknown;proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual expectedResult(RemoteInfo(RemoteNode.Unknown(None), None), false)
    }

    "preserve obfuscated ports on IP nodes with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="192.0.2.43:_hidden"
        """.stripMargin
      ) mustEqual expectedResult(
        RemoteInfo(RemoteNode.Ip(addr("192.0.2.43"), Some(NodePort.Obfuscated("_hidden"))), None),
        false
      )
    }

    "use unknown addresses with rfc7239 without inventing an IP address" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=unknown
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult(RemoteInfo(RemoteNode.Unknown(None), None), false)
    }

    "use unknown addresses with ports with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=unknown:1234
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult(RemoteInfo(RemoteNode.Unknown(Some(NodePort.Numeric(1234))), None), false)
    }

    "use unknown addresses case-insensitively with rfc7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=Unknown;proto=https
        """.stripMargin
      ) mustEqual expectedResult(RemoteInfo(RemoteNode.Unknown(None), None), true)
    }

    "ignore rfc7239 header with empty addresses" in {
      forwardedResultFrom(
        handler(version("rfc7239") ++ trustedProxies("192.0.2.43")),
        expectedResult("192.0.2.43", true),
        headers("""
                  |Forwarded: for=""
        """.stripMargin)
      ) mustEqual expectedResult("192.0.2.43", true)
    }

    "partly ignore rfc7239 header with some empty addresses" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=, for=
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.10", false)
    }

    "default to trusting IPv4 and IPv6 localhost with x-forwarded when there is no config" in {
      forwardedResultFrom(
        noConfigHandler,
        expectedResult(localhost, false),
        headers("""
                  |X-Forwarded-For: 192.0.2.43, ::1, 127.0.0.1, [::1]
                  |X-Forwarded-Proto: https, http, http, https
        """.stripMargin)
      ) mustEqual expectedResult("192.0.2.43", true)
    }

    "preserve the raw remote port when no forwarded headers are present" in {
      forwardedResultFrom(
        noConfigHandler,
        expectedResult(localhost, Some(12345), false),
        Headers()
      ) mustEqual expectedResult(localhost, Some(12345), false)
    }

    "clear the raw remote port when x-forwarded changes the remote address" in {
      forwardedResultToLocalhostWithPort(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        Some(12345),
        """
          |X-Forwarded-For: 192.0.2.43
        """.stripMargin
      ) mustEqual expectedResult("192.0.2.43", false)
    }

    "use the port from an rfc7239 forwarded IPv4 address" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="192.0.2.43:443";proto=https
        """.stripMargin
      ) mustEqual expectedResult("192.0.2.43", Some(443), true)
    }

    "use unquoted rfc7239 node ports for compatibility" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=192.0.2.43:443;by=[2001:db8:cafe::17]:8080
        """.stripMargin
      ) mustEqual expectedResult(
        RemoteInfo(
          RemoteNode.Ip(addr("192.0.2.43"), Some(NodePort.Numeric(443))),
          Some(RemoteNode.Ip(addr("2001:db8:cafe::17"), Some(NodePort.Numeric(8080))))
        ),
        false
      )
    }

    "use the port from an rfc7239 forwarded IPv6 address" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="[2001:db8:cafe::17]:443";proto=https
        """.stripMargin
      ) mustEqual expectedResult("2001:db8:cafe::17", Some(443), true)
    }

    "use the port from the first untrusted rfc7239 forwarded address" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="203.0.113.43:443"
          |Forwarded: for="192.168.1.43:9000"
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", Some(443), false)
    }

    "not use x-forwarded-port as the selected remote endpoint port" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Port: 443
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "use the port from an x-forwarded-for address" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43:443
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", Some(443), false)
    }

    "keep the source port from x-forwarded-for when x-forwarded-port is present" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43:53124
          |X-Forwarded-Port: 443
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", Some(53124), false)
    }

    "preserve the raw peer source port when only x-forwarded-port is present" in {
      forwardedResultToLocalhostWithPort(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        Some(53124),
        """
          |X-Forwarded-Port: 443
        """.stripMargin
      ) mustEqual expectedResult(localhost, Some(53124), false)
    }

    "trust IPv4 and IPv6 localhost with x-forwarded when there is config with default settings" in {
      forwardedResultToLocalhost(
        version("x-forwarded"),
        """
          |X-Forwarded-For: 192.0.2.43, ::1
          |X-Forwarded-Proto: https, https
        """.stripMargin
      ) mustEqual expectedResult("192.0.2.43", true)
    }

    "get first untrusted proxy with x-forwarded with subnet mask" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, http
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "not treat the first x-forwarded entry as a proxy even if it is in trustedProxies range" in {
      forwardedResultFrom(
        handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")),
        expectedResult(localhost, true),
        headers("""
                  |X-Forwarded-For: 192.168.1.2, 192.168.1.3
                  |X-Forwarded-Proto: http, http
        """.stripMargin)
      ) mustEqual expectedResult("192.168.1.2", false)
    }

    "fall back to the initial remote when single x-forwarded-for entry cannot be parsed" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: ???
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    // example from issue #5299
    "handle single unquoted IPv6 addresses in x-forwarded-for headers" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: ::1
        """.stripMargin
      ) mustEqual expectedResult("::1", false)
    }

    // example from RFC 7239 section 7.4
    "handle unquoted IPv6 addresses in x-forwarded-for headers" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1", "2001:db8:cafe::17"),
        """
          |X-Forwarded-For: 192.0.2.43, 2001:db8:cafe::17
        """.stripMargin
      ) mustEqual expectedResult("192.0.2.43", false)
    }

    // We're really forgiving about quoting for X-Forwarded-For headers,
    // since there isn't a real spec to follow.
    "handle lots of different IPv6 address quoting in x-forwarded-for headers" in {
      forwardedResultToLocalhost(
        version("x-forwarded"),
        """
          |X-Forwarded-For: 192.0.2.43, "::1", ::1, "[::1]", [::1]
        """.stripMargin
      ) mustEqual expectedResult("192.0.2.43", false)
    }

    // We're really forgiving about quoting for X-Forwarded-For headers,
    // since there isn't a real spec to follow.
    "ignore x-forward header with empty addresses" in {
      forwardedResultFrom(
        handler(version("x-forwarded") ++ trustedProxies("192.0.2.43")),
        expectedResult("192.0.2.43", true),
        headers("""
                  |X-Forwarded-For: ,,
        """.stripMargin)
      ) mustEqual expectedResult("192.0.2.43", true)
    }

    "partly ignore x-forward header with some empty addresses" in {
      forwardedResultToLocalhost(
        version("x-forwarded"),
        """
          |X-Forwarded-For: ,,192.0.2.43
        """.stripMargin
      ) mustEqual expectedResult("192.0.2.43", false)
    }

    "return the first address if all addresses are trusted with RFC 7239" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=192.168.1.12, for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.12", false)
    }

    "return the first address if all addresses are trusted with X-Forwarded-For" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 192.168.1.12, "192.168.1.10", 127.0.0.1
          |X-Forwarded-Proto: http, http, http
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.12", false)
    }

  }
}
