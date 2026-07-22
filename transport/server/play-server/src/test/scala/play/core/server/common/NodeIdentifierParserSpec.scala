/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import com.google.common.net.InetAddresses
import org.specs2.mutable.Specification
import ForwardedHeaderHandler.ForwardedHeaderVersion
import ForwardedHeaderHandler.Rfc7239
import ForwardedHeaderHandler.Xforwarded
import NodeIdentifierParser._

class NodeIdentifierParserSpec extends Specification {
  def parseNode(version: ForwardedHeaderVersion, str: String) = {
    val parser = new NodeIdentifierParser(version)
    parser.parseNode(str)
  }

  private def ip(s: String): Ip = Ip(InetAddresses.forString(s))

  "NodeIdentifierParser" should {
    "parse an ip v6 address with port" in {
      parseNode(Rfc7239, "[8F:F3B::FF]:9000") must beRight(ip("8F:F3B::FF") -> Some(PortNumber(9000)))
    }

    "not parse unescaped ip v6 address in rfc7239 header" in {
      parseNode(Rfc7239, "8F:F3B::FF") must beLeft
    }

    "parse unescaped ip v6 address in x-forwarded-for header" in {
      parseNode(Xforwarded, "8F:F3B::FF") must beRight(ip("8F:F3B::FF") -> None)
    }

    "parse an ip v6 address with obfuscated port" in {
      parseNode(Rfc7239, "[::FF]:_obf") must beRight(ip("::FF") -> Some(ObfuscatedPort("_obf")))
    }

    "parse IPv4-mapped IPv6 nodes without changing their historical identity representation" in {
      parseNode(Rfc7239, "[::ffff:192.0.2.43]:4711") must beRight(
        ip("::ffff:192.0.2.43") -> Some(PortNumber(4711))
      )
      parseNode(Xforwarded, "::ffff:192.0.2.43") must beRight(ip("::ffff:192.0.2.43") -> None)
    }

    "parse an ip v4 address with port" in {
      parseNode(Rfc7239, "127.0.0.1:8080") must beRight(ip("127.0.0.1") -> Some(PortNumber(8080)))
    }

    "parse an ip v4 address without port" in {
      parseNode(Rfc7239, "192.168.0.1") must beRight(ip("192.168.0.1") -> None)
    }

    "reject bracketed ip v4 addresses" in {
      parseNode(Rfc7239, "[192.168.0.1]") must beLeft
      parseNode(Xforwarded, "[192.168.0.1]") must beLeft
    }

    "parse an unknown ip address without port" in {
      parseNode(Rfc7239, "unknown") must beRight(UnknownIp -> None)
    }

    "parse an unknown ip address case-insensitively" in {
      parseNode(Rfc7239, "Unknown") must beRight(UnknownIp -> None)
      parseNode(Rfc7239, "UNKNOWN") must beRight(UnknownIp -> None)
    }

    "parse an obfuscated ip address without port" in {
      parseNode(Rfc7239, "_harry") must beRight(ObfuscatedIp("_harry") -> None)
    }

    "reject whitespace around rfc7239 node identifiers" in {
      parseNode(Rfc7239, " _harry") must beLeft
      parseNode(Rfc7239, "_harry ") must beLeft
      parseNode(Rfc7239, " 127.0.0.1 ") must beLeft
      parseNode(Rfc7239, "[::1] : _port") must beLeft
    }

    "retain surrounding whitespace compatibility for x-forwarded-for" in {
      parseNode(Xforwarded, " 127.0.0.1 ") must beRight(ip("127.0.0.1") -> None)
    }

    "reject scoped and non-ASCII IP spellings" in {
      Seq("[fe80::1%1]", "[fe80::1%eth0]", "[fe80::1%25eth0]").forall { value =>
        parseNode(Rfc7239, value).isLeft
      } must beTrue
      parseNode(Rfc7239, "１２７.０.０.１") must beLeft
      parseNode(Xforwarded, "١٢٧.٠.٠.١") must beLeft
    }
  }
}
