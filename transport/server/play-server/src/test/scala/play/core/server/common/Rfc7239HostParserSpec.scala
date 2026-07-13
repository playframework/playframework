/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import org.specs2.mutable.Specification

class Rfc7239HostParserSpec extends Specification {

  "Rfc7239HostParser" should {
    "accept HTTP Host field values" in {
      val validHosts = Seq(
        "example.com",
        "example.com:443",
        "[2001:db8::1]",
        "[2001:db8::1]:8443",
        "[v1.fe80::a]:443",
        "exa%6Dple.com",
        "name!$&'()*+,;=.example",
        "",
        ":"
      )

      validHosts.forall(host => Rfc7239HostParser.parse(host).contains(host)) must beTrue
    }

    "reject values outside the HTTP Host field grammar" in {
      val invalidHosts = Seq(
        "user@example.com",
        "example.com/path",
        "example.com?query",
        "example.com#fragment",
        "example .com",
        "example.com:http",
        "example.com:443:extra",
        "2001:db8::1",
        "[2001:db8::1",
        "[not-an-ip]",
        "[v1.]",
        "exa%mple.com",
        "m\u00fcnich.example"
      )

      invalidHosts.forall(host => Rfc7239HostParser.parse(host).isEmpty) must beTrue
    }
  }
}
