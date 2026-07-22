/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.Inet6Address

import scala.util.Try

import com.google.common.net.InetAddresses
import org.specs2.matcher.DataTables
import org.specs2.mutable.Specification

class SubnetSpec extends Specification with DataTables {
  "Subnet" should {
    "check if ip is in range" in {
      "Subnet" || "IpAddress" | "is in range" |
        "127.0.0.1" !! "127.0.0.1" ! true |
        "192.168.5.6/24" !! "192.168.5.1" ! true |
        "192.168.100.0/22" !! "192.168.103.255" ! true |
        "192.168.100.0/22" !! "192.168.104.1" ! false |
        "fe80::/64" !! "fe80::54ff:fffe:32fe" ! true |
        "2001:db8::/32" !! "2001:db9::1" ! false |
        "2001:dbfe::/31" !! "2001:dbff::" ! true |
        "2001:dbfe::/31" !! "2001:dbff::" ! true |
        "0.0.0.0/0" !! "203.0.113.43" ! true |
        "192.0.2.43/32" !! "192.0.2.43" ! true |
        "::/0" !! "2001:db8::1" ! true |
        "2001:db8::1/128" !! "2001:db8::1" ! true |
        "2001:db8:cafe::17" !! "2001:db8:cafe::17" ! true |> { (a, b, c) =>
          Subnet(a).isInRange(InetAddresses.forString(b)) mustEqual c
        }
    }

    "reject invalid CIDR prefix lengths and empty prefixes" in {
      val invalid = Seq(
        "127.0.0.1/-1",
        "127.0.0.1/33",
        "127.0.0.1/99999999999",
        "127.0.0.1/",
        "127.0.0.1/+1",
        "::1/-1",
        "::1/129"
      )

      invalid.forall(value => Try(Subnet(value)).isFailure) must beTrue
    }

    "report oversized CIDR prefix lengths consistently" in {
      Subnet("127.0.0.1/99999999999") must throwA[IllegalArgumentException](
        message = "127.0.0.1/99999999999 has an invalid CIDR prefix length."
      )
    }

    "retain IPv4-mapped IPv6 trust matching compatibility" in {
      Subnet("::ffff:192.0.2.43").isInRange(InetAddresses.forString("::ffff:192.0.2.43")) must beTrue
    }

    "reject scoped and non-ASCII configured addresses" in {
      val invalid = Seq(
        "fe80::1%1",
        "fe80::1%eth0",
        "fe80::1%25eth0",
        "１２７.０.０.１",
        "١٢٧.٠.٠.١"
      )

      invalid.forall(value => Try(Subnet(value)).isFailure) must beTrue
    }

    "reject scoped addresses supplied programmatically" in {
      val unscoped = InetAddresses.forString("fe80::1").asInstanceOf[Inet6Address]
      val scoped   = Seq(0, 1).map(scope => Inet6Address.getByAddress(null, unscoped.getAddress, scope))

      scoped.forall(value => Try(Subnet(value)).isFailure) must beTrue
    }
  }
}
