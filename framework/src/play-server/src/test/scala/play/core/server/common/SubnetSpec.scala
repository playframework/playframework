/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.common

import java.net.InetAddress

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
        "2001:db8:cafe::17" !! "2001:db8:cafe::17" ! true |>
        {
          (a, b, c) => Subnet(a).isInRange(InetAddress.getByName(b)) mustEqual c
        }
    }
  }
}
