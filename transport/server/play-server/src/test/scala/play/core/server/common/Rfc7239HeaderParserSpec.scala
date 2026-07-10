/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import org.specs2.mutable.Specification

class Rfc7239HeaderParserSpec extends Specification {

  "Rfc7239HeaderParser" should {
    "parse elements, parameters, and optional list whitespace" in {
      Rfc7239HeaderParser.parse("\tfor=192.0.2.43;proto=https \t,\t for=198.51.100.17\t") must beRight(
        Vector(
          Map("for" -> "192.0.2.43", "proto" -> "https"),
          Map("for" -> "198.51.100.17")
        )
      )
    }

    "parse every token character" in {
      Rfc7239HeaderParser.parse("!#$%&'*+-.^_`|~=value") must beRight(
        Vector(Map("!#$%&'*+-.^_`|~" -> "value"))
      )
    }

    "parse quoted text, separators, and quoted-pair escapes" in {
      Rfc7239HeaderParser.parse("""for="_edge\",still";proto="ht\\tps"""") must beRight(
        Vector(Map("for" -> "_edge\",still", "proto" -> "ht\\tps"))
      )
    }

    "parse empty parameter slots" in {
      Rfc7239HeaderParser.parse(";for=192.0.2.43;;proto=https;") must beRight(
        Vector(Map("for" -> "192.0.2.43", "proto" -> "https"))
      )
    }

    "parse legacy unquoted for and by node values" in {
      Rfc7239HeaderParser.parse(
        "for=[2001:db8:cafe::17]:4711;by=192.0.2.43:8080"
      ) must beRight(
        Vector(
          Map(
            "for" -> "[2001:db8:cafe::17]:4711",
            "by"  -> "192.0.2.43:8080"
          )
        )
      )
    }

    "ignore empty HTTP list elements" in {
      Rfc7239HeaderParser.parse(", ,for=192.0.2.43,,") must beRight(
        Vector(Map("for" -> "192.0.2.43"))
      )
    }

    "parse a field line containing only empty HTTP list elements for later recombination" in {
      Seq("", ",", ", \t,").forall(
        Rfc7239HeaderParser.parse(_) == Right(Vector.empty[Map[String, String]])
      ) must beTrue
    }

    "parse obs-text in quoted strings and quoted-pairs" in {
      Rfc7239HeaderParser.parse("proto=\"\u00ff\\\u00fe\"") must beRight(
        Vector(Map("proto" -> "\u00ff\u00fe"))
      )
    }

    "reject duplicate parameters case-insensitively" in {
      Rfc7239HeaderParser.parse("for=192.0.2.43;For=198.51.100.17") must beLeft
    }

    "reject whitespace outside HTTP list separators" in {
      Seq(
        "for =192.0.2.43",
        "for= 192.0.2.43",
        "for=192.0.2.43 ;proto=https",
        "for=192.0.2.43; proto=https"
      ).forall(Rfc7239HeaderParser.parse(_).isLeft) must beTrue
    }

    "reject values that are neither tokens nor quoted strings" in {
      Seq(
        "proto=https:443",
        "extension=value:part",
        "for=192.0.2.43/24",
        "for=\"192.0.2.43\"suffix"
      ).forall(Rfc7239HeaderParser.parse(_).isLeft) must beTrue
    }

    "reject malformed quoted strings" in {
      Seq(
        "for=\"unterminated",
        "for=\"invalid\u0007text\"",
        "for=\"invalid\\\u0007pair\"",
        "for=\"invalid\u0100text\""
      ).forall(Rfc7239HeaderParser.parse(_).isLeft) must beTrue
    }

    "reject non-ASCII token characters" in {
      Rfc7239HeaderParser.parse("f\u00f6r=192.0.2.43") must beLeft
    }
  }
}
