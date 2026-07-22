/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.math.BigInteger
import java.net.Inet6Address
import java.util.Optional

import scala.util.Try

import org.specs2.mutable.Specification

class RequestAuthoritySpec extends Specification {
  "RequestAuthority" should {
    "parse and normalize registered names without resolving them" in {
      RequestAuthority.parse("EXAMPLE.%63om") must beRight.like {
        case authority =>
          authority.host must beEqualTo(AuthorityHost.RegName("example.com"))
          authority.port must beNone
          authority.render must_== "example.com"
      }

      RequestAuthority.parse("not-a-dns-name") must beRight.like {
        case authority =>
          authority.host must beAnInstanceOf[AuthorityHost.RegName]
      }
      RequestAuthority.parse("999.999.999.999") must beRight.like {
        case authority =>
          authority.host must beAnInstanceOf[AuthorityHost.RegName]
      }
    }

    "normalize percent encoding in registered names" in {
      RequestAuthority.parse("%45XAMPLE%2eCOM%2f") must beRight.like {
        case authority =>
          authority.render must_== "example.com%2F"
      }
    }

    "classify and normalize IPv4 literals" in {
      RequestAuthority.parse("192.0.2.43:8080") must beRight.like {
        case authority =>
          authority.host must beAnInstanceOf[AuthorityHost.IPv4]
          authority.port must beSome(AuthorityPort(8080))
          authority.render must_== "192.0.2.43:8080"
      }
    }

    "classify, bracket, and normalize IPv6 literals" in {
      RequestAuthority.parse("[2001:0DB8:0:0:0:0:0:1]:443") must beRight.like {
        case authority =>
          authority.host must beAnInstanceOf[AuthorityHost.IPv6]
          authority.render must_== "[2001:db8::1]:443"
      }
    }

    "classify IPv4-mapped IPv6 literals by their URI syntax" in {
      RequestAuthority.parse("[::ffff:192.0.2.43]") must beRight.like {
        case authority =>
          authority.host must beAnInstanceOf[AuthorityHost.IPv6]
          authority.render must_== "[::ffff:c000:22b]"
      }

      AuthorityHost.ipv6("::ffff:192.0.2.43").render must_== "[::ffff:c000:22b]"
      AuthorityHost.ipv4("::ffff:192.0.2.43") must throwA[IllegalArgumentException]
    }

    "enforce the RFC 3986 IPv6 address grammar" in {
      val valid = Seq(
        "[::]",
        "[::1]",
        "[1::]",
        "[1:2:3:4:5:6:192.0.2.43]",
        "[1:2:3:4:5::192.0.2.43]"
      )
      val invalid = Seq(
        "[1:2:3:4:5:6:7]",
        "[1:2:3:4:5:6:7:8:9]",
        "[1::2::3]",
        "[:::1]",
        "[192.0.2.43::]",
        "[::ffff:192.0.2.999]",
        "[::ffff:192.168.001.1]"
      )

      valid.forall(RequestAuthority.parse(_).isRight) must beTrue
      invalid.forall(RequestAuthority.parse(_).isLeft) must beTrue
    }

    "classify and case-normalize IPvFuture literals" in {
      val upper = RequestAuthority.parse("[VF.FOO:BAR]:8443").toOption.get
      val lower = RequestAuthority.parse("[vf.foo:bar]:8443").toOption.get

      upper must beEqualTo(lower)
      upper.host must beEqualTo(lower.host)
      upper.hashCode must_== lower.hashCode
      upper.host.hashCode must_== lower.host.hashCode
      RequestAuthority.parse("[VF.FOO:BAR]:8443") must beRight.like {
        case authority =>
          authority.host must beEqualTo(AuthorityHost.IPvFuture("vf.foo:bar"))
          authority.render must_== "[vf.foo:bar]:8443"
      }
    }

    "provide validated host factories without DNS lookup" in {
      AuthorityHost.regName("EXAMPLE.%63om").render must_== "example.com"
      AuthorityHost.ipv4("192.0.2.43").render must_== "192.0.2.43"
      AuthorityHost.ipv6("2001:0DB8::1").render must_== "[2001:db8::1]"
      AuthorityHost.ipvFuture("VF.FOO:BAR").render must_== "[vf.foo:bar]"

      AuthorityHost.ipv4("localhost") must throwA[IllegalArgumentException]
      AuthorityHost.ipv6("192.0.2.43") must throwA[IllegalArgumentException]
    }

    "represent an empty textual port as an absent port" in {
      RequestAuthority.parse("example.com:") must beRight(RequestAuthority(AuthorityHost.RegName("example.com"), None))
      RequestAuthority.parse("[2001:db8::1]:").map(_.render) must beRight("[2001:db8::1]")
    }

    "preserve port zero and normalize leading zeroes" in {
      RequestAuthority.parse("example.com:00000") must beRight.like {
        case authority =>
          authority.port must beSome(AuthorityPort(0))
          authority.port.flatMap(_.tcpPort) must beSome(0)
          authority.render must_== "example.com:0"
      }
    }

    "support arbitrary non-negative decimal URI ports" in {
      val value = BigInt("123456789012345678901234567890")

      RequestAuthority.parse(s"example.com:$value") must beRight.like {
        case authority =>
          authority.port must beSome(AuthorityPort(value))
          authority.port.flatMap(_.tcpPort) must beNone
          authority.render must_== s"example.com:$value"
      }
    }

    "allow the empty registered name admitted by the URI grammar" in {
      RequestAuthority.parse("") must beRight(RequestAuthority(AuthorityHost.RegName(""), None))
      RequestAuthority.parse(":") must beRight(RequestAuthority(AuthorityHost.RegName(""), None))
      RequestAuthority.parse(":0").map(_.render) must beRight(":0")
    }

    "replace or remove a parsed port explicitly" in {
      val authority = RequestAuthority.parse("example.com:80").toOption.get

      authority.withPort(AuthorityPort(0)).render must_== "example.com:0"
      authority.withPort(Some(AuthorityPort(0))).render must_== "example.com:0"
      authority.withPort(None).render must_== "example.com"
    }

    "offer throwing and Java-optional constructors" in {
      val host = AuthorityHost.regName("example.com")

      RequestAuthority.parseOrThrow("EXAMPLE.COM:443").render must_== "example.com:443"
      RequestAuthority.create(host, Optional.of(AuthorityPort(443))).render must_== "example.com:443"
      RequestAuthority.create(host, Optional.empty[AuthorityPort]()).render must_== "example.com"
      RequestAuthority.parseOrThrow("user@example.com") must throwA[IllegalArgumentException]
    }

    "reject userinfo, paths, queries, fragments, and whitespace" in {
      val invalidAuthorities = Seq(
        "user@example.com",
        "example.com/path",
        "example.com?query",
        "example.com#fragment",
        "example.com ",
        " example.com"
      )

      invalidAuthorities.forall(RequestAuthority.parse(_).isLeft) must beTrue
    }

    "reject malformed IP-literal brackets" in {
      val invalidAuthorities = Seq(
        "[2001:db8::1",
        "2001:db8::1",
        "[]",
        "[192.0.2.43]",
        "[2001:db8::1]extra",
        "[2001:db8::1]]",
        "[v1.]",
        "[v1.a?]"
      )

      invalidAuthorities.forall(RequestAuthority.parse(_).isLeft) must beTrue
    }

    "reject scoped IPv6 literals and non-ASCII IP spellings" in {
      val invalidAuthorities = Seq(
        "[fe80::1%1]",
        "[fe80::1%eth0]",
        "[fe80::1%25eth0]",
        "１２７.０.０.１",
        "١٢٧.٠.٠.١"
      )

      invalidAuthorities.forall(RequestAuthority.parse(_).isLeft) must beTrue
      Seq("fe80::1%1", "fe80::1%eth0", "fe80::1%25eth0").forall { value =>
        Try(AuthorityHost.ipv6(value)).isFailure
      } must beTrue
    }

    "reject scoped IPv6 values supplied programmatically" in {
      val address = AuthorityHost.ipv6("fe80::1").address
      val scoped  = Seq(0, 1).map(scope => Inet6Address.getByAddress(null, address.getAddress, scope))

      scoped.forall(value => Try(AuthorityHost.IPv6(value)).isFailure) must beTrue
      scoped.forall(value => Try(AuthorityHost.ipv6(value)).isFailure) must beTrue
    }

    "reject invalid ports" in {
      val invalidAuthorities = Seq(
        "example.com:-1",
        "example.com:+1",
        "example.com:1.0",
        "example.com: 80",
        "example.com:١"
      )

      invalidAuthorities.forall(RequestAuthority.parse(_).isLeft) must beTrue
    }

    "reject invalid registered-name percent encoding" in {
      Seq("example.%", "example.%2", "example.%GG").forall(RequestAuthority.parse(_).isLeft) must beTrue
    }

    "return a descriptive error rather than throwing for invalid input" in {
      RequestAuthority.parse(null) must beLeft("Invalid authority: null")
      RequestAuthority.parse("user@example.com") must beLeft.like {
        case error =>
          error must contain("Invalid authority")
          error must contain("registered name")
      }
    }
  }

  "AuthorityPort" should {
    "project only the TCP/UDP port range" in {
      AuthorityPort(0).tcpPort must beSome(0)
      AuthorityPort(65535).tcpPort must beSome(65535)
      AuthorityPort(65536).tcpPort must beNone
      AuthorityPort(BigInt(Int.MaxValue) + 1).tcpPort must beNone
    }

    "parse decimal values and normalize leading zeroes" in {
      AuthorityPort.parse("00080") must beRight(AuthorityPort(80))
      AuthorityPort.parse("00080").map(_.render) must beRight("80")
    }

    "support Java arbitrary-precision construction" in {
      val value = new BigInteger("123456789012345678901234567890")

      AuthorityPort.create(value).render must_== value.toString
      AuthorityPort.parseOrThrow("00080") must_== AuthorityPort(80)
      AuthorityPort.parseOrThrow("-1") must throwA[IllegalArgumentException]
    }

    "reject empty, signed, non-ASCII, and non-decimal values" in {
      Seq("", "-1", "+1", "1.0", " 80", "١").forall(AuthorityPort.parse(_).isLeft) must beTrue
      AuthorityPort.parse(null) must beLeft("Invalid authority port: null")
    }

    "reject negative programmatic values" in {
      AuthorityPort(-1) must throwA[IllegalArgumentException]
    }
  }
}
