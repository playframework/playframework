/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import org.specs2.mutable.Specification

class AcceptEncodingSpec extends Specification {
  val E = EncodingPreference
  "AcceptEncoding.parseHeader" should {
    "parse simple Accept-Encoding headers" in {
      AcceptEncoding.parseHeader("gzip, compress, br").sorted must_==
        Seq(E("br"), E("compress"), E("gzip"))
    }
    "parse Accept-Encoding headers with q-values" in {
      AcceptEncoding.parseHeader("gzip; q=0.5, compress, br").sorted must_==
        Seq(E("br"), E("compress"), E("gzip", Some(0.5)))
    }
    "parse Accept-Encoding headers with wildcard" in {
      AcceptEncoding.parseHeader("deflate, gzip;q=1.0, *;q=0.5").sorted must_==
        Seq(E("deflate"), E("gzip", Some(1.0)), E("*", Some(0.5)))
    }
    "sort in correct order" in {
      AcceptEncoding.parseHeader("identity;q=0, gzip;q=0.5, br;q=1.0").sorted must_==
        Seq(E("br", Some(1.0)), E("gzip", Some(0.5)), E("identity", Some(0)))
    }
    "handle invalid parts gracefully" in {
      AcceptEncoding.parseHeader("compress;q, br;q=0.5, gzip").sorted must_==
        Seq(E("compress"), E("gzip"), E("br", Some(0.5)))
    }
  }
  "AcceptEncoding#preferred" should {
    "get preferred encoding with one available encoding" in {
      AcceptEncoding("gzip").preferred(Seq("gzip")) must beSome("gzip")
    }
    "get preferred encoding with no q-values" in {
      AcceptEncoding("gzip, compress, br").preferred(Seq("br", "gzip")) must beSome("br")
    }
    "match uppercase encoding names" in {
      AcceptEncoding("gZip, compress, BR").preferred(Seq("br", "gzip")) must beSome("br")
    }
    "match uppercase Q" in {
      AcceptEncoding("gZip;Q=0.5, compress;Q=1, br;Q=1").preferred(Seq("br", "gzip")) must beSome("br")
    }
    "get preferred encoding with q-values" in {
      AcceptEncoding("gzip; q=0.5, compress, br").preferred(Seq("br", "compress", "deflate")) must beSome("br")
    }
    "get identity encoding with no available encodings" in {
      AcceptEncoding("").preferred(Seq("br", "compress", "deflate")) must beSome("identity")
    }
    "get identity encoding with no matching encodings" in {
      AcceptEncoding("gzip, compress").preferred(Seq("br", "deflate")) must beSome("identity")
    }
    "get no encoding with no matching encodings and explicit identity;q=0" in {
      AcceptEncoding("gzip, compress; q=0.3, identity;q=0.0").preferred(Seq("br", "deflate")) must beNone
    }
    "get preferred encoding with multiple headers" in {
      AcceptEncoding("gzip; q=0.5, compress", "br")
        .preferred(Seq("br", "compress", "deflate")) must beSome("br")
    }
    "get preferred encoding with duplicate headers" in {
      AcceptEncoding("gzip; q=0.5, br; q=0.75", "gzip; q=1", "gzip; q=0.2")
        .preferred(Seq("br", "gzip", "deflate")) must beSome("gzip")
    }
    "get preferred encoding with duplicate headers" in {
      AcceptEncoding("gzip; q=0.5, br; q=0.75", "gzip; q=1", "gzip; q=0.2")
        .preferred(Seq("br", "gzip", "deflate")) must beSome("gzip")
    }
    "get less preferred encoding when it's the only one we support" in {
      AcceptEncoding("gzip; q=0.5, br; q=0.75, compress; q=0.1")
        .preferred(Seq("compress", "deflate")) must beSome("compress")
    }
    "get identity encoding when only * matches" in {
      AcceptEncoding("gzip; q=0.5, br; q=0.75", "*; q=0.1")
        .preferred(Seq("deflate")) must beSome("identity")
    }
  }
}
