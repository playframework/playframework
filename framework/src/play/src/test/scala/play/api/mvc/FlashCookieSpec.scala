/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.net.URLEncoder

import org.specs2.specification.core.Fragments

class FlashCookieSpec extends org.specs2.mutable.Specification {
  "Flash cookies" should {
    "bake in a header and value" in {
      val es = flash.encode(Map("a" -> "b"))
      val m = flash.decode(es)

      m must haveSize(1) and {
        m.get("a") must beSome("b")
      }
    }

    "bake in multiple headers and values" in {
      val es = flash.encode(Map("a" -> "b", "c" -> "d"))
      val m = flash.decode(es)

      m must haveSize(2) and {
        m.get("a") must beSome("b")
        m.get("c") must beSome("d")
      }
    }

    "bake in a header an empty value" in {
      val es = flash.encode(Map("a" -> ""))
      val m = flash.decode(es)

      m must haveSize(1)
      m.get("a") must beSome("")
    }

    "bake in a header a Unicode value" in {
      val es = flash.encode(Map("a" -> "\u0000"))
      val m = flash.decode(es)

      m must haveSize(1)
      m.get("a") must beSome("\u0000")
    }

    "bake in an empty map" in {
      val es = flash.encode(Map.empty)
      val m = flash.decode(es)

      m must beEmpty
    }

    "encode values such that no extra keys can be created" in {
      val es = flash.encode(Map("a" -> "b&c=d"))
      val m = flash.decode(es)

      m must haveSize(1)
      m.get("a") must beSome("b&c=d")
    }

    "specifically exclude control chars" in {
      for (i <- 0 until 32) {
        val s = Character.toChars(i).toString
        val es = flash.encode(Map("a" -> s))
        es must not contain s

        val m = flash.decode(es)
        m must haveSize(1)
        m.get("a") must beSome(s)
      }
      success
    }

    "specifically exclude special cookie chars" in {
      val es = flash.encode(Map("a" -> " \",;\\"))

      es must not contain " "
      es must not contain "\""
      es must not contain ","
      es must not contain ";"
      es must not contain "\\"

      val m = flash.decode(es)

      m must haveSize(1)
      m.get("a") must beSome(" \",;\\")
    }

    "decode values of the previously supported format" in {
      val es = oldEncoder(Map("a" -> "b", "c" -> "d"))

      flash.decode(es) must beEmpty
    }

    "decode values of the previously supported format with the new delimiters in them" in {
      val es = oldEncoder(Map("a" -> "b&="))

      flash.decode(es) must beEmpty
    }

    "decode values with gibberish in them" in {
      flash.decode("asfjdlkasjdflk") must beEmpty
    }

    "put disallows null values" in {
      val c = Flash(Map("foo" -> "bar"))
      c + (("x", null)) must throwA(new IllegalArgumentException("requirement failed: Flash value for x cannot be null"))
    }

    "be insecure by default" in {
      flash.encodeAsCookie(Flash()).secure must beFalse
    }

    "decode pair with value including '='" in {
      flash.decode("a=foo=bar&b=lorem") must_== Map(
        "a" -> "foo=bar",
        "b" -> "lorem"
      )
    }
  }

  // ---

  def oldEncoder(data: Map[String, String]): String = {
    URLEncoder.encode(
      data.map(d => d._1 + ":" + d._2).mkString("\u0000"),
      "UTF-8"
    )
  }

  def flash: FlashCookieBaker = new DefaultFlashCookieBaker()
}
