/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.common

import java.net.InetAddress
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.Headers
import play.api.{ PlayException, Configuration }
import play.core.server.common.ForwardedHeaderHandler._

class ForwardedHeaderHandlerSpec extends Specification {

  "ForwardedHeaderHandler" should {
    """not accept a wrong setting as "play.http.forwarded.version" in config""" in {
      handler(version("rfc7240")) must throwA[PlayException]
    }

    "parse rfc7239 entries" in {
      val results = processHeaders(version("rfc7239") ++ trustedProxies("192.0.2.60/24"), headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=198.51.100.17, for=127.0.0.1
          |Forwarded: for=192.0.2.61;proto=https
          |Forwarded: for=unknown
        """.stripMargin
      ))
      results.length must_== 8
      results(0)._1 must_== ForwardedEntry(Some("_gazonk"), None)
      results(0)._2 must beLeft
      results(0)._3 must beNone
      results(1)._1 must_== ForwardedEntry(Some("[2001:db8:cafe::17]:4711"), None)
      results(1)._2 must beRight(ConnectionInfo(addr("2001:db8:cafe::17"), false))
      results(1)._3 must beSome(false)
      results(2)._1 must_== ForwardedEntry(Some("192.0.2.60"), Some("http"))
      results(2)._2 must beRight(ConnectionInfo(addr("192.0.2.60"), false))
      results(2)._3 must beSome(true)
      results(3)._1 must_== ForwardedEntry(Some("192.0.2.43"), None)
      results(3)._2 must beRight(ConnectionInfo(addr("192.0.2.43"), false))
      results(3)._3 must beSome(true)
      results(4)._1 must_== ForwardedEntry(Some("198.51.100.17"), None)
      results(4)._2 must beRight(ConnectionInfo(addr("198.51.100.17"), false))
      results(4)._3 must beSome(false)
      results(5)._1 must_== ForwardedEntry(Some("127.0.0.1"), None)
      results(5)._2 must beRight(ConnectionInfo(addr("127.0.0.1"), false))
      results(5)._3 must beSome(false)
      results(6)._1 must_== ForwardedEntry(Some("192.0.2.61"), Some("https"))
      results(6)._2 must beRight(ConnectionInfo(addr("192.0.2.61"), true))
      results(6)._3 must beSome(true)
      results(7)._1 must_== ForwardedEntry(Some("unknown"), None)
      results(7)._2 must beLeft
      results(7)._3 must beNone
    }

    "parse x-forwarded entries" in {
      val results = processHeaders(version("x-forwarded") ++ trustedProxies("2001:db8:cafe::17"), headers(
        """
          |X-Forwarded-For: 192.168.1.1, ::1, [2001:db8:cafe::17], 127.0.0.1
          |X-Forwarded-Proto: https, http, https, http
        """.stripMargin
      ))
      results.length must_== 4
      results(0)._1 must_== ForwardedEntry(Some("192.168.1.1"), Some("https"))
      results(0)._2 must beRight(ConnectionInfo(addr("192.168.1.1"), true))
      results(0)._3 must beSome(false)
      results(1)._1 must_== ForwardedEntry(Some("::1"), Some("http"))
      results(1)._2 must beRight(ConnectionInfo(addr("::1"), false))
      results(1)._3 must beSome(false)
      results(2)._1 must_== ForwardedEntry(Some("[2001:db8:cafe::17]"), Some("https"))
      results(2)._2 must beRight(ConnectionInfo(addr("2001:db8:cafe::17"), true))
      results(2)._3 must beSome(true)
      results(3)._1 must_== ForwardedEntry(Some("127.0.0.1"), Some("http"))
      results(3)._2 must beRight(ConnectionInfo(addr("127.0.0.1"), false))
      results(3)._3 must beSome(false)
    }

    "default to trusting IPv4 and IPv6 localhost with rfc7239 when there is config with default settings" in {
      handler(version("rfc7239")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for=192.0.2.43;proto=https, for="[::1]"
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), true)
    }

    "ignore proxy hosts with rfc7239 when no proxies are trusted" in {
      handler(version("rfc7239") ++ trustedProxies()).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=198.51.100.17, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(localhost, false)
    }

    "get first untrusted proxy host with rfc7239 with ipv4 localhost" in {
      handler(version("rfc7239")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=198.51.100.17, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("198.51.100.17"), false)
    }

    "get first untrusted proxy host with rfc7239 with ipv6 localhost" in {
      handler(version("rfc7239")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=[::1]
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), false)
    }

    "get first untrusted proxy with rfc7239 with trusted proxy subnet" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.60"), false)
    }

    "get first untrusted proxy protocol with rfc7239 with trusted localhost proxy" in {
      handler(version("rfc7239") ++ trustedProxies("127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    "get first untrusted proxy protocol with rfc7239 with subnet mask" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=https;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.60"), true)
    }

    "handle IPv6 addresses with rfc7239" in {
      handler(version("rfc7239") ++ trustedProxies("127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: For=[2001:db8:cafe::17]:4711
        """.stripMargin)) mustEqual ConnectionInfo(addr("2001:db8:cafe::17"), false)
    }

    "handle quoted IPv6 addresses with rfc7239" in {
      handler(version("rfc7239") ++ trustedProxies("127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: For="[2001:db8:cafe::17]:4711"
        """.stripMargin)) mustEqual ConnectionInfo(addr("2001:db8:cafe::17"), false)
    }

    "ignore obfuscated addresses with rfc7239" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    "ignore unknown addresses with rfc7239" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for=unknown
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    "ignore rfc7239 header with empty addresses" in {
      handler(version("rfc7239") ++ trustedProxies("192.0.2.43")).remoteConnection(addr("192.0.2.43"), true, headers(
        """
          |Forwarded: for=""
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), true)
    }

    "partly ignore rfc7239 header with some empty addresses" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for=, for=
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    "ignore rfc7239 header field with missing = sign" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    "ignore rfc7239 header field with two == signs" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for==
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    // This quotation handling is not RFC-compliant but we want to make sure we
    // at least handle the case gracefully.
    "don't unquote rfc7239 header field with one \" character" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for==
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    // This quotation handling is not RFC-compliant but we want to make sure we
    // at least handle the case gracefully.
    "unquote and ignore rfc7239 empty quoted header field" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for=""
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    // This quotation handling is not RFC-compliant but we want to make sure we
    // at least handle the case gracefully.
    "kind of unquote rfc7239 header field with three \" characters" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for=""" + '"' + '"' + '"' + """
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    "default to trusting IPv4 and IPv6 localhost with x-forwarded when there is no config" in {
      noConfigHandler.remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 192.0.2.43, ::1, 127.0.0.1, [::1]
          |X-Forwarded-Proto: https, http, http, https
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), true)
    }

    "trust IPv4 and IPv6 localhost with x-forwarded when there is config with default settings" in {
      handler(version("x-forwarded")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 192.0.2.43, ::1
          |X-Forwarded-Proto: https, https
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), true)
    }

    "get first untrusted proxy with x-forwarded with subnet mask" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, http
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), true)
    }

    "not treat the first x-forwarded entry as a proxy even if it is in trustedProxies range" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, true, headers(
        """
          |X-Forwarded-For: 192.168.1.2, 192.168.1.3
          |X-Forwarded-Proto: http, http
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.2"), false)
    }

    "assume http protocol with x-forwarded when proto list is missing" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), false)
    }

    "assume http protocol with x-forwarded when proto list is shorter than for list" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), false)
    }

    "assume http protocol with x-forwarded when proto list is shorter than for list and all addresses are trusted" in {
      handler(version("x-forwarded") ++ trustedProxies("0.0.0.0/0")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), false)
    }

    "assume http protocol with x-forwarded when proto list is longer than for list" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24", "127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, https, https
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), false)
    }

    "assume http protocol with x-forwarded when proto is unrecognized" in {
      handler(version("x-forwarded") ++ trustedProxies("127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43
          |X-Forwarded-Proto: smtp
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), false)
    }

    "fall back to connection when single x-forwarded-for entry cannot be parsed" in {
      handler(version("x-forwarded") ++ trustedProxies("127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: ???
        """.stripMargin)) mustEqual ConnectionInfo(localhost, false)
    }

    // example from issue #5299
    "handle single unquoted IPv6 addresses in x-forwarded-for headers" in {
      handler(version("x-forwarded") ++ trustedProxies("127.0.0.1")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: ::1
        """.stripMargin)) mustEqual ConnectionInfo(addr("::1"), false)
    }

    // example from RFC 7239 section 7.4
    "handle unquoted IPv6 addresses in x-forwarded-for headers" in {
      handler(version("x-forwarded") ++ trustedProxies("127.0.0.1", "2001:db8:cafe::17")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 192.0.2.43, 2001:db8:cafe::17
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), false)
    }

    // We're really forgiving about quoting for X-Forwarded-For headers,
    // since there isn't a real spec to follow.
    "handle lots of different IPv6 address quoting in x-forwarded-for headers" in {
      handler(version("x-forwarded")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 192.0.2.43, "::1", ::1, "[::1]", [::1]
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), false)
    }

    // We're really forgiving about quoting for X-Forwarded-For headers,
    // since there isn't a real spec to follow.
    "handle lots of different IPv6 address and proto quoting in x-forwarded-for headers" in {
      handler(version("x-forwarded")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 192.0.2.43, "::1", ::1, "[::1]", [::1]
          |X-Forwarded-Proto: "https", http, http,    "http", http
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), true)
    }

    "ignore x-forward header with empty addresses" in {
      handler(version("x-forwarded") ++ trustedProxies("192.0.2.43")).remoteConnection(addr("192.0.2.43"), true, headers(
        """
          |X-Forwarded-For: ,,
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), true)
    }

    "partly ignore x-forward header with some empty addresses" in {
      handler(version("x-forwarded")).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: ,,192.0.2.43
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.43"), false)
    }

  }

  def noConfigHandler =
    new ForwardedHeaderHandler(ForwardedHeaderHandlerConfig(None))

  def handler(config: Map[String, Any]) =
    new ForwardedHeaderHandler(ForwardedHeaderHandlerConfig(Some(Configuration.reference ++ Configuration.from(config))))

  def version(s: String) = {
    Map("play.http.forwarded.version" -> s)
  }

  def trustedProxies(s: String*) = {
    Map("play.http.forwarded.trustedProxies" -> s)
  }

  def headers(s: String): Headers = {

    def split(s: String, regex: String): Option[(String, String)] = s.split(regex, 2).toList match {
      case k :: v :: Nil => Some(k -> v)
      case _ => None
    }

    new Headers(s.split("\n").flatMap(split(_, ":\\s*")))
  }

  def processHeaders(config: Map[String, Any], headers: Headers): Seq[(ForwardedEntry, Either[String, ConnectionInfo], Option[Boolean])] = {
    val configuration = ForwardedHeaderHandlerConfig(Some(Configuration.from(config)))
    configuration.forwardedHeaders(headers).map { forwardedEntry =>
      val errorOrConnection = configuration.parseEntry(forwardedEntry)
      val trusted = errorOrConnection match {
        case Left(_) => None
        case Right(connection) => Some(configuration.isTrustedProxy(connection))
      }
      (forwardedEntry, errorOrConnection, trusted)
    }
  }

  def addr(ip: String): InetAddress = InetAddress.getByName(ip)

  val localhost: InetAddress = addr("127.0.0.1")

}
