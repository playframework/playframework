/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import org.specs2.mutable.Specification

class SchemeSpec extends Specification {
  "Scheme" should {
    "parse and normalize RFC 3986 schemes" in {
      Scheme.parse("HTTP") must beRight(Scheme.Http)
      Scheme.parse("hTtPs") must beRight(Scheme.Https)
      Scheme.parse("Git+SSH.v1-2") must beRight.like {
        case scheme =>
          scheme.value must_== "git+ssh.v1-2"
          scheme.render must_== "git+ssh.v1-2"
          scheme.toString must_== "git+ssh.v1-2"
      }
    }

    "identify only HTTPS as secure" in {
      Scheme.Http.isSecure must beFalse
      Scheme.Https.isSecure must beTrue
      Scheme.parse("wss").map(_.isSecure) must beRight(false)
    }

    "offer a throwing constructor for Java and programmatic use" in {
      Scheme.create("HTTPS") must_== Scheme.Https
      Scheme.parseOrThrow("Git+SSH").render must_== "git+ssh"
      Scheme.parseOrThrow("not a scheme") must throwA[IllegalArgumentException]
    }

    "reject strings outside the RFC 3986 scheme grammar" in {
      val invalidSchemes = Seq("", "1http", "+http", "http:", "http/2", "http scheme", "höttöp")

      invalidSchemes.forall(Scheme.parse(_).isLeft) must beTrue
      Scheme.parse(null) must beLeft("Invalid scheme: null")
    }
  }
}
