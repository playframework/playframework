/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.InetAddress

import com.google.common.net.InetAddresses
import org.specs2.mutable.Specification
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RemoteConnection.RemoteNode
import play.api.mvc.Headers
import play.api.Configuration
import play.api.PlayException
import play.core.server.common.ForwardedHeaderHandler._

class ForwardedHeaderHandlerSpec extends Specification {
  "ForwardedHeaderHandler" should {
    """not accept a wrong setting as "play.http.forwarded.version" in config""" in {
      handler(version("rfc7240")) must throwA[PlayException]
    }

    "accept valid trusted proxy identifiers in config" in {
      val identifiers   = Seq("_edge", "_A0._-")
      val configuration = ForwardedHeaderHandlerConfig(
        Some(
          Configuration
            .from(version("rfc7239") ++ trustedProxyIdentifiers(identifiers*))
            .withFallback(Configuration.reference)
        )
      )

      configuration.trustedProxyIdentifiers mustEqual identifiers.toSet
    }

    "reject unknown as a trusted proxy identifier in config" in {
      handler(version("rfc7239") ++ trustedProxyIdentifiers("unknown")) must throwA[PlayException]
      handler(version("rfc7239") ++ trustedProxyIdentifiers("UNKNOWN")) must throwA[PlayException]
    }

    "reject malformed trusted proxy identifiers in config" in {
      def isRejected(identifier: String): Boolean = {
        try {
          handler(version("rfc7239") ++ trustedProxyIdentifiers(identifier))
          false
        } catch {
          case _: PlayException => true
        }
      }

      Seq("edge", "_", "_bad!", "_ümlaut").forall(isRejected) must beTrue
    }

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
      results(0)._2 must beRight(ParsedForwardedEntry(RemoteNode.Obfuscated("_gazonk", None), None, None, false))
      results(0)._3 must beNone
      results(1)._1 must_== ForwardedEntry(Some("[2001:db8:cafe::17]:4711"), None)
      results(1)._2 must beRight(
        ParsedForwardedEntry(RemoteNode.Ip(addr("2001:db8:cafe::17"), Some(4711)), None, Some(4711), false)
      )
      results(1)._3 must beSome(false)
      results(2)._1 must_== ForwardedEntry(Some("192.0.2.60"), Some("http"), byString = Some("203.0.113.43"))
      results(2)._2 must beRight(
        ParsedForwardedEntry(
          RemoteNode.Ip(addr("192.0.2.60"), None),
          None,
          None,
          false,
          Some(RemoteNode.Ip(addr("203.0.113.43"), None))
        )
      )
      results(2)._3 must beSome(true)
      results(3)._1 must_== ForwardedEntry(Some("192.0.2.43"), None)
      results(3)._2 must beRight(ParsedForwardedEntry(RemoteNode.Ip(addr("192.0.2.43"), None), None, None, false))
      results(3)._3 must beSome(true)
      results(4)._1 must_== ForwardedEntry(Some("198.51.100.17"), None)
      results(4)._2 must beRight(ParsedForwardedEntry(RemoteNode.Ip(addr("198.51.100.17"), None), None, None, false))
      results(4)._3 must beSome(false)
      results(5)._1 must_== ForwardedEntry(Some("127.0.0.1"), None)
      results(5)._2 must beRight(ParsedForwardedEntry(RemoteNode.Ip(addr("127.0.0.1"), None), None, None, false))
      results(5)._3 must beSome(false)
      results(6)._1 must_== ForwardedEntry(Some("192.0.2.61"), Some("https"))
      results(6)._2 must beRight(ParsedForwardedEntry(RemoteNode.Ip(addr("192.0.2.61"), None), None, None, true))
      results(6)._3 must beSome(true)
      results(7)._1 must_== ForwardedEntry(Some("unknown"), None)
      results(7)._2 must beRight(ParsedForwardedEntry(RemoteNode.Unknown(None), None, None, false))
      results(7)._3 must beNone
      results(8)._1 must_== ForwardedEntry(Some("[::ffff:192.168.0.9]"), Some("https"))
      results(8)._2 must beRight(
        ParsedForwardedEntry(RemoteNode.Ip(addr("::ffff:192.168.0.9"), None), None, None, true)
      )
      results(8)._3 must beSome(false)
    }

    "parse rfc7239 by entries without changing selected remote identity" in {
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
          RemoteNode.Ip(addr("192.0.2.43"), None),
          None,
          None,
          true,
          Some(RemoteNode.Ip(addr("203.0.113.43"), None))
        )
      )
      results(1)._2 must beRight(
        ParsedForwardedEntry(
          RemoteNode.Ip(addr("192.0.2.44"), None),
          None,
          None,
          false,
          Some(RemoteNode.Obfuscated("_edge", None))
        )
      )
      results(2)._2 must beRight(
        ParsedForwardedEntry(
          RemoteNode.Ip(addr("192.0.2.45"), None),
          None,
          None,
          false,
          Some(RemoteNode.Unknown(None))
        )
      )
      results(3)._2 must beRight(
        ParsedForwardedEntry(
          RemoteNode.Ip(addr("192.0.2.46"), None),
          None,
          None,
          false,
          Some(RemoteNode.Ip(addr("2001:db8:cafe::17"), Some(4711)))
        )
      )
      results(4)._2 must beRight(
        ParsedForwardedEntry(RemoteNode.Ip(addr("192.0.2.47"), None), None, None, false, None)
      )
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
          RemoteNode.Obfuscated("_edge.1", None),
          None,
          None,
          false,
          Some(RemoteNode.Obfuscated("_proxy-1", None))
        )
      )
    }

    "parse rfc7239 lists and parameters with separators inside quoted strings" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies(),
        headers(
          """
            |Forwarded: for="_bad,stillbad";proto=https, for=203.0.113.43
            |Forwarded: for="_bad;stillbad";proto=https, for=198.51.100.17
            |Forwarded: for="_bad=stillbad";proto=https, for=192.0.2.43
          """.stripMargin
        )
      )

      results.map(_._1) must containTheSameElementsAs(
        Seq(
          ForwardedEntry(Some("_bad,stillbad"), Some("https")),
          ForwardedEntry(Some("203.0.113.43"), None),
          ForwardedEntry(Some("_bad;stillbad"), Some("https")),
          ForwardedEntry(Some("198.51.100.17"), None),
          ForwardedEntry(Some("_bad=stillbad"), Some("https")),
          ForwardedEntry(Some("192.0.2.43"), None)
        )
      )
    }

    "parse all rfc7239 token characters" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies(),
        headers(
          """
            |Forwarded: !#$%&'*+-.^_`|~=value;for=203.0.113.43
          """.stripMargin
        )
      )

      results.map(_._1) must containTheSameElementsAs(Seq(ForwardedEntry(Some("203.0.113.43"), None)))
    }

    "parse empty rfc7239 parameter slots" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies(),
        headers(
          """
            |Forwarded: ;for=203.0.113.43;;proto=https;
          """.stripMargin
        )
      )

      results.map(_._1) must containTheSameElementsAs(
        Seq(ForwardedEntry(Some("203.0.113.43"), Some("https")))
      )
    }

    "ignore empty rfc7239 list elements" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: , , for=203.0.113.43, , for=127.0.0.1, ,
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "ignore empty rfc7239 list elements split over header fields" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: , ,
          |Forwarded: for=203.0.113.43, for=127.0.0.1
          |Forwarded: ,
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "reject combined rfc7239 fields containing only empty list elements" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies(),
        headers(
          """
            |Forwarded: , ,
            |Forwarded: ,
          """.stripMargin
        )
      )

      results must haveSize(1)
      results.head._1 must_== ForwardedEntry(None, None)
      results.head._2 must beLeft
    }

    "reject duplicate rfc7239 parameters case-insensitively" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies(),
        headers(
          """
            |Forwarded: for=203.0.113.43;For=198.51.100.17
          """.stripMargin
        )
      )

      results must haveSize(1)
      results.head._1 must_== ForwardedEntry(None, None)
      results.head._2 must beLeft
    }

    "stop trusted proxy scanning at a malformed rfc7239 field" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43
          |Forwarded: for=198.51.100.17;for=192.0.2.43
          |Forwarded: for=192.168.1.43, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.43", false, None)
    }

    "reject a complete rfc7239 field when one of its elements is malformed" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43, for=127.0.0.1;for=192.0.2.43
        """.stripMargin
      ) mustEqual RemoteConnection(localhost, None, secure = false, None)
    }

    "reject whitespace around rfc7239 parameter separators" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies(),
        headers(
          """
            |Forwarded: for =203.0.113.43
            |Forwarded: for=203.0.113.43 ;proto=https
            |Forwarded: for=203.0.113.43; proto=https
          """.stripMargin
        )
      )

      results.map(_._1) must containTheSameElementsAs(Seq.fill(3)(ForwardedEntry(None, None)))
    }

    "expose rfc7239 by entries on the selected remote connection" in {
      val connection = remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;by=192.0.2.10;proto=https
        """.stripMargin
      )

      connection.remoteNode must beEqualTo(RemoteNode.Ip(addr("203.0.113.43"), None))
      connection.byNode must beSome(RemoteNode.Ip(addr("192.0.2.10"), None))
      connection.secure must beTrue
    }

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

      forwarding.host must beSome("public.example:9443")
      forwarding.connection.remoteIdentity mustEqual "203.0.113.43"
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

    "not reuse rfc7239 host from a different forwarded entry" in {
      forwardedHostToLocalhost(
        version("rfc7239") ++
          trustedProxies("127.0.0.1", "192.168.1.1/24") ++
          trustForwardedHost(true),
        """
          |Host: internal.example
          |Forwarded: for=203.0.113.43
          |Forwarded: for=192.168.1.43;host=proxy.example
        """.stripMargin
      ) must beNone
    }

    "ignore invalid rfc7239 host values" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
        """
          |Host: internal.example
          |Forwarded: for=203.0.113.43;proto=https;host="user@example.org"
        """.stripMargin
      )

      forwarding.host must beNone
      forwarding.connection.remoteIdentity mustEqual "203.0.113.43"
      forwarding.connection.secure must beTrue
    }

    "use rfc7239 host without for from a directly trusted proxy" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
        """
          |Forwarded: host=public.example
        """.stripMargin
      )

      forwarding.host must beSome("public.example")
      forwarding.connection.remoteIdentity mustEqual "127.0.0.1"
    }

    "stop scanning before entries preceding an rfc7239 host without for" in {
      val forwarding = forwardedRequestToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true),
        """
          |Forwarded: for=203.0.113.43;host=client.example, host=proxy.example
        """.stripMargin
      )

      forwarding.host must beSome("proxy.example")
      forwarding.connection.remoteIdentity mustEqual "127.0.0.1"
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
      results(0)._2 must beRight(ParsedForwardedEntry(RemoteNode.Ip(addr("192.168.1.1"), None), None, None, true))
      results(0)._3 must beSome(false)
      results(1)._1 must_== ForwardedEntry(Some("::1"), Some("http"))
      results(1)._2 must beRight(ParsedForwardedEntry(RemoteNode.Ip(addr("::1"), None), None, None, false))
      results(1)._3 must beSome(false)
      results(2)._1 must_== ForwardedEntry(Some("[2001:db8:cafe::17]"), Some("https"))
      results(2)._2 must beRight(ParsedForwardedEntry(RemoteNode.Ip(addr("2001:db8:cafe::17"), None), None, None, true))
      results(2)._3 must beSome(true)
      results(3)._1 must_== ForwardedEntry(Some("127.0.0.1"), Some("http"))
      results(3)._2 must beRight(ParsedForwardedEntry(RemoteNode.Ip(addr("127.0.0.1"), None), None, None, false))
      results(3)._3 must beSome(false)
      results(4)._1 must_== ForwardedEntry(Some("::ffff:123.123.123.123"), Some("https"))
      results(4)._2 must beRight(
        ParsedForwardedEntry(RemoteNode.Ip(addr("::ffff:123.123.123.123"), None), None, None, true)
      )
      results(4)._3 must beSome(false)
    }

    "default to trusting IPv4 and IPv6 localhost with rfc7239 when there is config with default settings" in {
      remoteConnectionToLocalhost(
        version("rfc7239"),
        """
          |Forwarded: for=192.0.2.43;proto=https, for="[::1]"
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", true, None)
    }

    "ignore proxy hosts with rfc7239 when no proxies are trusted" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies(),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=198.51.100.17, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection(localhost, false, None)
    }

    "get first untrusted proxy host with rfc7239 with ipv4 localhost" in {
      remoteConnectionToLocalhost(
        version("rfc7239"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=198.51.100.17, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("198.51.100.17", false, None)
    }

    "get first untrusted proxy host with rfc7239 with ipv6 localhost" in {
      remoteConnectionToLocalhost(
        version("rfc7239"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=[::1]
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", false, None)
    }

    "get first untrusted proxy with rfc7239 with trusted proxy subnet" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection(
        addr("192.0.2.60"),
        RemoteNode.Ip(addr("192.0.2.60"), None),
        None,
        Some(RemoteNode.Ip(addr("203.0.113.43"), None)),
        secure = false,
        None
      )
    }

    "get first untrusted proxy protocol with rfc7239 with trusted localhost proxy" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.10", false, None)
    }

    "get first untrusted proxy protocol with rfc7239 with subnet mask" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=https;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection(
        addr("192.0.2.60"),
        RemoteNode.Ip(addr("192.0.2.60"), None),
        None,
        Some(RemoteNode.Ip(addr("203.0.113.43"), None)),
        secure = true,
        None
      )
    }

    "treat rfc7239 proto schemes case-insensitively" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;proto=HtTpS
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", secure = true, None)
    }

    "handle unquoted IPv6 addresses with rfc7239 for compatibility" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: For=[2001:db8:cafe::17]:4711
        """.stripMargin
      ) mustEqual RemoteConnection("2001:db8:cafe::17", Some(4711), secure = false, None)
    }

    "handle quoted IPv6 addresses with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: For="[2001:db8:cafe::17]:4711"
        """.stripMargin
      ) mustEqual RemoteConnection("2001:db8:cafe::17", Some(4711), secure = false, None)
    }

    "handle quoted IPv4-mapped IPv6 addresses with rfc7239" in {
      handler(version("rfc7239") ++ trustedProxies("fe80::1", "::ffff:123.123.123.123"))
        .forwardedConnection(
          RemoteConnection("fe80::1", false, None),
          headers("""
                    |Forwarded: For="[::ffff:99.99.99.99]:4711"
                    |Forwarded: For="[::ffff:123.123.123.123]"
        """.stripMargin)
        ) mustEqual RemoteConnection(addr("::ffff:99.99.99.99"), Some(4711), secure = false, None)
    }

    "use obfuscated addresses with rfc7239 and keep the previous IP address as fallback" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="_gazonk"
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection(addr("192.168.1.10"), RemoteNode.Obfuscated("_gazonk", None), None, false, None)
    }

    "use proto from obfuscated addresses with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="_gazonk";proto=https
        """.stripMargin
      ) mustEqual RemoteConnection(addr("127.0.0.1"), RemoteNode.Obfuscated("_gazonk", None), None, secure = true, None)
    }

    "stop scanning at obfuscated addresses with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;proto=https
          |Forwarded: for="_gazonk";proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual RemoteConnection(
        addr("192.168.1.10"),
        RemoteNode.Obfuscated("_gazonk", None),
        None,
        secure = false,
        None
      )
    }

    "not reuse proto from a trusted obfuscated proxy for the client with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++
          trustedProxies("192.168.1.1/24", "127.0.0.1") ++
          trustedProxyIdentifiers("_edge"),
        """
          |Forwarded: for=203.0.113.43
          |Forwarded: for="_edge";proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "continue scanning through trusted obfuscated identifiers with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++
          trustedProxies("192.168.1.1/24", "127.0.0.1") ++
          trustedProxyIdentifiers("_edge"),
        """
          |Forwarded: for=203.0.113.43;proto=https
          |Forwarded: for="_edge";proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", true, None)
    }

    "match trusted obfuscated identifiers exactly with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++
          trustedProxies("192.168.1.1/24", "127.0.0.1") ++
          trustedProxyIdentifiers("_edge"),
        """
          |Forwarded: for=203.0.113.43;proto=https
          |Forwarded: for="_edge2";proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual RemoteConnection(
        addr("192.168.1.10"),
        RemoteNode.Obfuscated("_edge2", None),
        None,
        secure = false,
        None
      )
    }

    "stop scanning at unknown identifiers with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43;proto=https
          |Forwarded: for=unknown;proto=http
          |Forwarded: for=192.168.1.10
        """.stripMargin
      ) mustEqual RemoteConnection(addr("192.168.1.10"), RemoteNode.Unknown(None), None, secure = false, None)
    }

    "ignore obfuscated ports with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="192.0.2.43:_hidden"
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", false, None)
    }

    "use unknown addresses with rfc7239 and keep the previous IP address as fallback" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=unknown
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection(addr("192.168.1.10"), RemoteNode.Unknown(None), None, false, None)
    }

    "use unknown addresses with ports with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=unknown:1234
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection(addr("192.168.1.10"), RemoteNode.Unknown(Some("1234")), None, false, None)
    }

    "use unknown addresses case-insensitively with rfc7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=Unknown;proto=https
        """.stripMargin
      ) mustEqual RemoteConnection(addr("127.0.0.1"), RemoteNode.Unknown(None), None, secure = true, None)
    }

    "ignore rfc7239 header with empty addresses" in {
      handler(version("rfc7239") ++ trustedProxies("192.0.2.43"))
        .forwardedConnection(
          RemoteConnection("192.0.2.43", true, None),
          headers("""
                    |Forwarded: for=""
        """.stripMargin)
        ) mustEqual RemoteConnection("192.0.2.43", true, None)
    }

    "partly ignore rfc7239 header with some empty addresses" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=, for=
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.10", false, None)
    }

    "ignore rfc7239 header field with missing = sign" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.10", false, None)
    }

    "ignore rfc7239 header field with two == signs" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for==
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.10", false, None)
    }

    "ignore an unterminated rfc7239 quoted value" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.10", false, None)
    }

    "ignore an empty rfc7239 quoted value" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=""
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.10", false, None)
    }

    "ignore a malformed rfc7239 value with three quote characters" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=""" + '"' + '"' + '"' + """
                                                    |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.10", false, None)
    }

    "not decode quoted-pair escapes in x-forwarded headers" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: "h\ttps"
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "default to trusting IPv4 and IPv6 localhost with x-forwarded when there is no config" in {
      noConfigHandler.forwardedConnection(
        RemoteConnection(localhost, false, None),
        headers("""
                  |X-Forwarded-For: 192.0.2.43, ::1, 127.0.0.1, [::1]
                  |X-Forwarded-Proto: https, http, http, https
        """.stripMargin)
      ) mustEqual RemoteConnection("192.0.2.43", true, None)
    }

    "preserve the raw remote port when no forwarded headers are present" in {
      noConfigHandler.forwardedConnection(
        RemoteConnection(localhost, Some(12345), secure = false, None),
        Headers()
      ) mustEqual RemoteConnection(localhost, Some(12345), secure = false, None)
    }

    "clear the raw remote port when x-forwarded changes the remote address" in {
      remoteConnectionToLocalhostWithPort(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        Some(12345),
        """
          |X-Forwarded-For: 192.0.2.43
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", false, None)
    }

    "use the port from an rfc7239 forwarded IPv4 address" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="192.0.2.43:443";proto=https
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", Some(443), secure = true, None)
    }

    "use unquoted rfc7239 node ports for compatibility" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=192.0.2.43:443;by=[2001:db8:cafe::17]:8080
        """.stripMargin
      ) mustEqual RemoteConnection(
        addr("192.0.2.43"),
        RemoteNode.Ip(addr("192.0.2.43"), Some(443)),
        Some(443),
        Some(RemoteNode.Ip(addr("2001:db8:cafe::17"), Some(8080))),
        secure = false,
        None
      )
    }

    "use the port from an rfc7239 forwarded IPv6 address" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for="[2001:db8:cafe::17]:443";proto=https
        """.stripMargin
      ) mustEqual RemoteConnection("2001:db8:cafe::17", Some(443), secure = true, None)
    }

    "use the port from the first untrusted rfc7239 forwarded address" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="203.0.113.43:443"
          |Forwarded: for="192.168.1.43:9000"
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", Some(443), secure = false, None)
    }

    "use x-forwarded-port when it matches a single x-forwarded-for address" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Port: 443
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", Some(443), secure = false, None)
    }

    "accept the maximum x-forwarded-port value" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Port: 65535
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", Some(65535), secure = false, None)
    }

    "ignore x-forwarded-port values above the maximum port value" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Port: 65536
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "use the port from an x-forwarded-for address" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43:443
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", Some(443), secure = false, None)
    }

    "prefer x-forwarded-port over the port in an x-forwarded-for address" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43:80
          |X-Forwarded-Port: 443
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", Some(443), secure = false, None)
    }

    "pair x-forwarded-port entries with x-forwarded-for entries" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Port: 443, 9000
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", Some(443), secure = false, None)
    }

    "ignore a single x-forwarded-port with multiple x-forwarded-for entries by default" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Port: 443
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "associate a single x-forwarded-port with the client when trustSingleXForwardedPort is enabled" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1") ++ trustSingleXForwardedPort(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Port: 443
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", Some(443), secure = false, None)
    }

    "ignore multiple x-forwarded-port entries shorter than x-forwarded-for" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0") ++ trustSingleXForwardedPort(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43, 192.168.1.44
          |X-Forwarded-Port: 443, 9000
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "ignore x-forwarded-port when x-forwarded-for is missing" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustSingleXForwardedPort(true),
        """
          |X-Forwarded-Port: 443
        """.stripMargin
      ) mustEqual RemoteConnection(localhost, false, None)
    }

    "ignore invalid x-forwarded-port values" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Port: 70000
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "ignore non-numeric x-forwarded-port values" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Port: abc
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "trust IPv4 and IPv6 localhost with x-forwarded when there is config with default settings" in {
      remoteConnectionToLocalhost(
        version("x-forwarded"),
        """
          |X-Forwarded-For: 192.0.2.43, ::1
          |X-Forwarded-Proto: https, https
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", true, None)
    }

    "get first untrusted proxy with x-forwarded with subnet mask" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, http
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", true, None)
    }

    "not treat the first x-forwarded entry as a proxy even if it is in trustedProxies range" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"))
        .forwardedConnection(
          RemoteConnection(localhost, true, None),
          headers("""
                    |X-Forwarded-For: 192.168.1.2, 192.168.1.3
                    |X-Forwarded-Proto: http, http
        """.stripMargin)
        ) mustEqual RemoteConnection("192.168.1.2", false, None)
    }

    "assume http protocol with x-forwarded when proto list is missing" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "treat x-forwarded-proto schemes case-insensitively" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: HtTpS
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", secure = true, None)
    }

    "assume http protocol with x-forwarded when proto list is shorter than for list" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "assume http protocol with x-forwarded when proto list is shorter than for list and all addresses are trusted" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "associate a single x-forwarded-proto with the client when trustSingleXForwardedProto is enabled" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", true, None)
    }

    "associate a single x-forwarded-proto with the client when trustSingleXForwardedProto is enabled and all addresses are trusted" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", true, None)
    }

    "associate a single x-forwarded-proto with the client when multiple forwarded-for entries are trusted" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43, 192.168.1.44
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", true, None)
    }

    "stop at an untrusted proxy when trustSingleXForwardedProto is enabled" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 198.51.100.17, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual RemoteConnection("198.51.100.17", false, None)
    }

    "assume http protocol with x-forwarded when proto list has multiple entries shorter than for list even when trustSingleXForwardedProto is enabled" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("0.0.0.0/0") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43, 192.168.1.44
          |X-Forwarded-Proto: https, https
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "not apply trustSingleXForwardedProto to rfc7239 forwarded headers" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1") ++ trustSingleXForwardedProto(true),
        """
          |Forwarded: for=203.0.113.43
          |Forwarded: for=192.168.1.43;proto=https
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "ignore single x-forwarded-proto when x-forwarded-for is missing even when trustSingleXForwardedProto is enabled" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustSingleXForwardedProto(true),
        """
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual RemoteConnection(localhost, false, None)
    }

    "use single x-forwarded-proto without x-forwarded-for when enabled and proxy is trusted" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual RemoteConnection(localhost, true, None)
    }

    "use single x-forwarded-proto without x-forwarded-for case-insensitively" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |X-Forwarded-Proto: HTTPS
        """.stripMargin
      ) mustEqual RemoteConnection(localhost, true, None)
    }

    "ignore single x-forwarded-proto without x-forwarded-for when proxy is untrusted" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.0.2.1") ++ trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |X-Forwarded-Proto: https
        """.stripMargin
      ) mustEqual RemoteConnection(localhost, false, None)
    }

    "ignore multiple x-forwarded-proto values without x-forwarded-for even when enabled" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1") ++ trustXForwardedProtoWithoutXForwardedFor(true),
        """
          |X-Forwarded-Proto: https, http
        """.stripMargin
      ) mustEqual RemoteConnection(localhost, false, None)
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

      forwarding.connection mustEqual RemoteConnection(localhost, false, None)
      forwarding.host must beSome("public.example")
    }

    "assume http protocol with x-forwarded when proto list is longer than for list" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, https, https
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "assume http protocol with x-forwarded when proto is unrecognized" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: smtp
        """.stripMargin
      ) mustEqual RemoteConnection("203.0.113.43", false, None)
    }

    "fall back to connection when single x-forwarded-for entry cannot be parsed" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: ???
        """.stripMargin
      ) mustEqual RemoteConnection(localhost, false, None)
    }

    // example from issue #5299
    "handle single unquoted IPv6 addresses in x-forwarded-for headers" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: ::1
        """.stripMargin
      ) mustEqual RemoteConnection("::1", false, None)
    }

    // example from RFC 7239 section 7.4
    "handle unquoted IPv6 addresses in x-forwarded-for headers" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1", "2001:db8:cafe::17"),
        """
          |X-Forwarded-For: 192.0.2.43, 2001:db8:cafe::17
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", false, None)
    }

    // We're really forgiving about quoting for X-Forwarded-For headers,
    // since there isn't a real spec to follow.
    "handle lots of different IPv6 address quoting in x-forwarded-for headers" in {
      remoteConnectionToLocalhost(
        version("x-forwarded"),
        """
          |X-Forwarded-For: 192.0.2.43, "::1", ::1, "[::1]", [::1]
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", false, None)
    }

    // We're really forgiving about quoting for X-Forwarded-For headers,
    // since there isn't a real spec to follow.
    "handle lots of different IPv6 address and proto quoting in x-forwarded-for headers" in {
      remoteConnectionToLocalhost(
        version("x-forwarded"),
        """
          |X-Forwarded-For: 192.0.2.43, "::1", ::1, "[::1]", [::1]
          |X-Forwarded-Proto: "https", http, http,    "http", http
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", true, None)
    }

    "ignore x-forward header with empty addresses" in {
      handler(version("x-forwarded") ++ trustedProxies("192.0.2.43"))
        .forwardedConnection(
          RemoteConnection("192.0.2.43", true, None),
          headers("""
                    |X-Forwarded-For: ,,
        """.stripMargin)
        ) mustEqual RemoteConnection("192.0.2.43", true, None)
    }

    "partly ignore x-forward header with some empty addresses" in {
      remoteConnectionToLocalhost(
        version("x-forwarded"),
        """
          |X-Forwarded-For: ,,192.0.2.43
        """.stripMargin
      ) mustEqual RemoteConnection("192.0.2.43", false, None)
    }

    "return the first address if all addresses are trusted with RFC 7239" in {
      remoteConnectionToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=192.168.1.12, for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.12", false, None)
    }

    "return the first address if all addresses are trusted with X-Forwarded-For" in {
      remoteConnectionToLocalhost(
        version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |X-Forwarded-For: 192.168.1.12, "192.168.1.10", 127.0.0.1
          |X-Forwarded-Proto: http, http, http
        """.stripMargin
      ) mustEqual RemoteConnection("192.168.1.12", false, None)
    }
  }

  def noConfigHandler =
    new ForwardedHeaderHandler(ForwardedHeaderHandlerConfig(None))

  def handler(config: Map[String, Any]) =
    new ForwardedHeaderHandler(
      ForwardedHeaderHandlerConfig(Some(Configuration.from(config).withFallback(Configuration.reference)))
    )

  def remoteConnectionToLocalhost(config: Map[String, Any], headersText: String): RemoteConnection =
    handler(config).forwardedConnection(RemoteConnection("127.0.0.1", false, None), headers(headersText))

  def forwardedHostToLocalhost(config: Map[String, Any], headersText: String): Option[String] =
    forwardedRequestToLocalhost(config, headersText).host

  def forwardedRequestToLocalhost(config: Map[String, Any], headersText: String): ParsedForwarding =
    handler(config).forwardedRequest(RemoteConnection("127.0.0.1", false, None), headers(headersText))

  def remoteConnectionToLocalhostWithPort(
      config: Map[String, Any],
      remotePort: Option[Int],
      headersText: String
  ): RemoteConnection =
    handler(config).forwardedConnection(
      RemoteConnection("127.0.0.1", remotePort, secure = false, None),
      headers(headersText)
    )

  def version(s: String) = {
    Map("play.http.forwarded.version" -> s)
  }

  def trustedProxies(s: String*) = {
    Map("play.http.forwarded.trustedProxies" -> s)
  }

  def trustSingleXForwardedProto(b: Boolean) = {
    Map("play.http.forwarded.trustSingleXForwardedProto" -> b)
  }

  def trustXForwardedProtoWithoutXForwardedFor(b: Boolean) = {
    Map("play.http.forwarded.trustXForwardedProtoWithoutXForwardedFor" -> b)
  }

  def trustSingleXForwardedPort(b: Boolean) = {
    Map("play.http.forwarded.trustSingleXForwardedPort" -> b)
  }

  def trustForwardedHost(b: Boolean) = {
    Map("play.http.forwarded.trustForwardedHost" -> b)
  }

  def trustedProxyIdentifiers(s: String*) = {
    Map("play.http.forwarded.trustedProxyIdentifiers" -> s)
  }

  def headers(s: String): Headers = {
    def split(s: String, regex: String): Option[(String, String)] = s.split(regex, 2).toList match {
      case k :: v :: Nil => Some(k -> v)
      case _             => None
    }

    new Headers(s.split("\r?\n").toSeq.flatMap(split(_, ":\\s*")))
  }

  def processHeaders(
      config: Map[String, Any],
      headers: Headers
  ): Seq[(ForwardedEntry, Either[String, ParsedForwardedEntry], Option[Boolean])] = {
    val configuration = ForwardedHeaderHandlerConfig(Some(Configuration.from(config)))
    configuration.forwardedHeaders(headers).map { forwardedEntry =>
      val errorOrConnection = configuration.parseEntry(forwardedEntry)
      val trusted           = errorOrConnection match {
        case Left(_)           => None
        case Right(connection) => connection.remoteIpAddress.map(configuration.isTrustedProxy)
      }
      (forwardedEntry, errorOrConnection, trusted)
    }
  }

  def addr(ip: String): InetAddress = InetAddresses.forString(ip)

  val localhost: InetAddress = addr("127.0.0.1")
}
