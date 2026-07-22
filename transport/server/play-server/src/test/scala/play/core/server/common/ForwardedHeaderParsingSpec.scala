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

class ForwardedHeaderParsingSpec extends Specification with ForwardedHeaderHandlerSpecSupport {
  private final class TrackingHeaders(values: Seq[(String, String)]) extends Headers(values) {
    private var _lookups = Vector.empty[String]

    def lookups: Vector[String] = _lookups

    override def getAll(key: String): Seq[String] = {
      _lookups :+= key
      super.getAll(key)
    }
  }

  "ForwardedHeaderHandler" should {
    """not accept a wrong setting as "play.http.forwarded.version" in config""" in {
      handler(version("rfc7240")) must throwA[PlayException]
    }

    "reject invalid trusted-proxy addresses and CIDR prefixes during configuration" in {
      val invalid = Seq(
        "127.0.0.1/-1",
        "127.0.0.1/33",
        "127.0.0.1/99999999999",
        "127.0.0.1/",
        "127.0.0.1/+1",
        "::1/-1",
        "::1/129",
        "fe80::1%1",
        "fe80::1%eth0",
        "fe80::1%25eth0",
        "１２７.０.０.１",
        "١٢٧.٠.٠.١"
      )

      invalid.forall(value => scala.util.Try(handler(trustedProxies(value))).isFailure) must beTrue
    }

    "not inspect malformed forwarding headers from an untrusted direct peer" in {
      val rawRemote        = RemoteInfo.ip("127.0.0.1", Some(NodePort.Numeric(53124)))
      val initialAuthority = Some(RequestAuthority.parseOrThrow("internal.example:9000"))
      val cases            = Seq(
        (
          ForwardedHeaderHandlerConfig(
            version = Rfc7239,
            trustedProxies = List.empty,
            trustForwardedHost = true
          ),
          Seq("Forwarded" -> """for=";proto=https;host=public.example""")
        ),
        (
          ForwardedHeaderHandlerConfig(
            version = Xforwarded,
            trustedProxies = List.empty,
            trustXForwardedProtoWithoutXForwardedFor = true,
            trustXForwardedPort = true,
            trustXForwardedHost = true,
            trustXForwardedSsl = true
          ),
          Seq(
            "X-Forwarded-For"   -> "???",
            "X-Forwarded-Proto" -> "1https",
            "X-Forwarded-Host"  -> "user@public.example",
            "X-Forwarded-Port"  -> "-1",
            "X-Forwarded-Ssl"   -> "invalid"
          )
        )
      )

      val results = cases.map {
        case (configuration, values) =>
          val requestHeaders = new TrackingHeaders(values)
          val result         = new ForwardedHeaderHandler(configuration)
            .forwardedRequest(rawRemote, requestHeaders, Scheme.Http, initialAuthority)
          result -> requestHeaders.lookups
      }

      results.map(_._1) mustEqual Seq.fill(cases.size)(
        ParsedForwarding(rawRemote, Scheme.Http, initialAuthority)
      )
      results.map(_._2) mustEqual Seq.fill(cases.size)(Vector.empty)
    }

    "parse forwarding headers when the direct peer is trusted" in {
      val requestHeaders = new TrackingHeaders(
        Seq("Forwarded" -> "for=203.0.113.43;proto=https;host=public.example")
      )
      val result = handler(
        version("rfc7239") ++ trustedProxies("127.0.0.1") ++ trustForwardedHost(true)
      ).forwardedRequest(
        RemoteInfo.ip("127.0.0.1", Some(NodePort.Numeric(53124))),
        requestHeaders,
        Scheme.Http,
        Some(RequestAuthority.parseOrThrow("internal.example:9000"))
      )

      result mustEqual ParsedForwarding(
        RemoteInfo
          .ip("203.0.113.43", None)
          .copy(
            forwarding = Some(
              play.api.mvc.request.ForwardingInfo(
                play.api.mvc.request.ForwardingSource.Rfc7239,
                Vector.empty
              )
            )
          ),
        Scheme.Https,
        Some(RequestAuthority.parseOrThrow("public.example"))
      )
      requestHeaders.lookups must contain("Forwarded")
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
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: , , for=203.0.113.43, , for=127.0.0.1, ,
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "ignore empty rfc7239 list elements split over header fields" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: , ,
          |Forwarded: for=203.0.113.43, for=127.0.0.1
          |Forwarded: ,
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
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
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43
          |Forwarded: for=198.51.100.17;for=192.0.2.43
          |Forwarded: for=192.168.1.43, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.43", false)
    }

    "reject a complete rfc7239 field when one of its elements is malformed" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("127.0.0.1"),
        """
          |Forwarded: for=203.0.113.43, for=127.0.0.1;for=192.0.2.43
        """.stripMargin
      ) mustEqual expectedResult(localhost, false)
    }

    "accept whitespace after rfc7239 parameter separators for Play 3.0 compatibility" in {
      Seq(" ", "\t", " \t  ").map { whitespace =>
        forwardedResultToLocalhost(
          version("rfc7239") ++ trustedProxies("127.0.0.1"),
          s"Forwarded: for=203.0.113.43;$whitespace" + "proto=https; extension=value"
        )
      } must containTheSameElementsAs(Seq.fill(3)(expectedResult("203.0.113.43", true)))
    }

    "reject whitespace before rfc7239 parameter separators and around equals signs" in {
      val results = processHeaders(
        version("rfc7239") ++ trustedProxies(),
        headers(
          """
            |Forwarded: for =203.0.113.43
            |Forwarded: for=203.0.113.43 ;proto=https
            |Forwarded: for=203.0.113.43;proto= https
          """.stripMargin
        )
      )

      results.map(_._1) must containTheSameElementsAs(Seq.fill(3)(ForwardedEntry(None, None)))
    }

    "ignore rfc7239 header field with missing = sign" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.10", false)
    }

    "ignore rfc7239 header field with two == signs" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for==
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.10", false)
    }

    "ignore an unterminated rfc7239 quoted value" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for="
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.10", false)
    }

    "ignore an empty rfc7239 quoted value" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=""
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.10", false)
    }

    "ignore a malformed rfc7239 value with three quote characters" in {
      forwardedResultToLocalhost(
        version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1"),
        """
          |Forwarded: for=""" + '"' + '"' + '"' + """
                                                    |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin
      ) mustEqual expectedResult("192.168.1.10", false)
    }

    "not decode quoted-pair escapes in x-forwarded headers" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: "h\ttps"
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", false)
    }

    "trim optional whitespace around quoted x-forwarded list members" in {
      forwardedResultToLocalhost(
        version("x-forwarded") ++ trustedProxies("127.0.0.1"),
        """
          |X-Forwarded-For: "203.0.113.43" 	,	 "127.0.0.1"
          |X-Forwarded-Proto: "https" 	,	 "http"
        """.stripMargin
      ) mustEqual expectedResult("203.0.113.43", true)
    }

    "parse unusually large X-Forwarded lists with adversarial separator whitespace" in {
      val elementCount  = 2048
      val separator     = "," + (" \t" * 32)
      val forValue      = Vector.fill(elementCount)("192.0.2.43").mkString(separator)
      val protoValue    = Vector.fill(elementCount)("https").mkString(separator)
      val configuration = ForwardedHeaderHandlerConfig(
        version = Xforwarded,
        trustedProxies = List.empty
      )

      val entries = configuration.forwardedHeaders(
        new Headers(
          Seq(
            "X-Forwarded-For"   -> forValue,
            "X-Forwarded-Proto" -> protoValue
          )
        )
      )

      entries.length must_== elementCount
      entries.head must_== ForwardedEntry(Some("192.0.2.43"), Some("https"))
      entries.last must_== ForwardedEntry(Some("192.0.2.43"), Some("https"))
    }
  }
}
