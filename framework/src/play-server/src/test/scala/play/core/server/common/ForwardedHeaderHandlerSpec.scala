/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.Headers
import play.api.{ PlayException, Configuration }
import play.core.server.common.ForwardedHeaderHandler.ForwardedHeaderHandlerConfig

class ForwardedHeaderHandlerSpec extends Specification {
  "ForwardedHeaderHandler" should {
    """not accept a wrong setting as "play.http.forwarded.version" in config""" in new TestData {
      handler(version("rfc7240")).remoteAddress(null) must throwA[PlayException]
    }

    "get first untrusted proxy host with rfc7239 with ipv4 loopback" in new TestData {
      handler(version("rfc7239")).remoteAddress(headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=198.51.100.17, for=127.0.0.1
        """.stripMargin)) mustEqual Some("198.51.100.17")
    }

    "get first untrusted proxy host with rfc7239 with ipv6 loopback" in new TestData {
      handler(version("rfc7239")).remoteAddress(headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.0.2.43, for=[::1]
        """.stripMargin)) mustEqual Some("192.0.2.43")
    }

    "get first untrusted proxy host with rfc7239 with subnet mask" in new TestData {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteAddress(headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual Some("192.0.2.60")
    }

    "get first untrusted proxy protocol with rfc7239" in new TestData {
      handler(version("rfc7239") ++ trustedProxies("127.0.0.1" :: Nil)).remoteProtocol(headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual None
    }

    "get first untrusted proxy protocol with rfc7239 with subnet mask" in new TestData {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteProtocol(headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=https;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual Some("https")
    }

    "get first untrusted proxy host with x-forwarded with subnet mask" in new TestData {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteAddress(headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, http
        """.stripMargin)) mustEqual Some("203.0.113.43")
    }

    "get first untrusted proxy protocol with x-forwarded with subnet mask" in new TestData {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteProtocol(headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, http
        """.stripMargin)) mustEqual Some("https")
    }
  }

  trait TestData extends Scope {
    def handler(config: Map[String, Any]) =
      new ForwardedHeaderHandler(ForwardedHeaderHandlerConfig(Some(Configuration.from(config))))

    def version(s: String) = {
      Map("play.http.forwarded.version" -> s)
    }

    def trustedProxies(s: List[String]) = {
      Map("play.http.forwarded.trustedProxies" -> s)
    }

    def headers(s: String) = {

      def split(s: String, regex: String): Option[(String, String)] = s.split(regex, 2).toList match {
        case k :: v :: Nil => Some(k -> v)
        case _ => None
      }

      new Headers(s.split("\n").flatMap(split(_, ":\\s*")))
    }
  }
}
