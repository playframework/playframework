/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import java.net.InetAddress
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.Headers
import play.api.{ PlayException, Configuration }
import play.core.server.common.ForwardedHeaderHandler.ForwardedHeaderHandlerConfig

class ForwardedHeaderHandlerSpec extends Specification {

  "ForwardedHeaderHandler" should {
    """not accept a wrong setting as "play.http.forwarded.version" in config""" in {
      handler(version("rfc7240")) must throwA[PlayException]
    }

    "ignore proxy hosts with rfc7239 when no proxies are trusted" in {
      handler(version("rfc7239") ++ trustedProxies(Nil)).remoteConnection(localhost, false, headers(
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
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.60"), false)
    }

    "get first untrusted proxy protocol with rfc7239 with trusted localhost proxy" in {
      handler(version("rfc7239") ++ trustedProxies("127.0.0.1" :: Nil)).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.10"), false)
    }

    "get first untrusted proxy protocol with rfc7239 with subnet mask" in {
      handler(version("rfc7239") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteConnection(localhost, false, headers(
        """
          |Forwarded: for="_gazonk"
          |Forwarded: For="[2001:db8:cafe::17]:4711"
          |Forwarded: for=192.0.2.60;proto=https;by=203.0.113.43
          |Forwarded: for=192.168.1.10, for=127.0.0.1
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.0.2.60"), true)
    }

    "get first untrusted proxy with x-forwarded with subnet mask" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, http
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), true)
    }

    "not treat the first x-forwarded entry as a proxy even if it is in trustedProxies range" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteConnection(localhost, true, headers(
        """
          |X-Forwarded-For: 192.168.1.2, 192.168.1.3
          |X-Forwarded-Proto: http, http
        """.stripMargin)) mustEqual ConnectionInfo(addr("192.168.1.2"), false)
    }

    "assume http protocol with x-forwarded when proto list is missing" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), false)
    }

    "assume http protocol with x-forwarded when proto list is shorter than for list" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), false)
    }

    "assume http protocol with x-forwarded when proto list is shorter than for list and all addresses are trusted" in {
      handler(version("x-forwarded") ++ trustedProxies("0.0.0.0/0" :: Nil)).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), false)
    }

    "assume http protocol with x-forwarded when proto list is longer than for list" in {
      handler(version("x-forwarded") ++ trustedProxies("192.168.1.1/24" :: "127.0.0.1" :: Nil)).remoteConnection(localhost, false, headers(
        """
          |X-Forwarded-For: 203.0.113.43, 192.168.1.43
          |X-Forwarded-Proto: https, https, https
        """.stripMargin)) mustEqual ConnectionInfo(addr("203.0.113.43"), false)
    }

  }

  def handler(config: Map[String, Any]) =
    new ForwardedHeaderHandler(ForwardedHeaderHandlerConfig(Some(Configuration.from(config))))

  def version(s: String) = {
    Map("play.http.forwarded.version" -> s)
  }

  def trustedProxies(s: List[String]) = {
    Map("play.http.forwarded.trustedProxies" -> s)
  }

  def headers(s: String): Headers = {

    def split(s: String, regex: String): Option[(String, String)] = s.split(regex, 2).toList match {
      case k :: v :: Nil => Some(k -> v)
      case _ => None
    }

    new Headers(s.split("\n").flatMap(split(_, ":\\s*")))
  }

  def addr(ip: String): InetAddress = InetAddress.getByName(ip)

  val localhost: InetAddress = addr("127.0.0.1")

}
