/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import org.specs2.mutable._

object HttpSpec extends Specification {
  "HTTP" title

  val headers = new Headers {
    val data = Seq(("a", Seq("a1", "a2")), ("b", Seq("b1", "b2")))
  }

  "Headers" should {
    "return the header value associated with a" in {
      headers.get("a") must beSome("a1")
    }

    "return the header values associated with b" in {
      headers.getAll("b") must be_==(Seq("b1", "b2"))
    }

    "not return an empty sequence of values associated with an unknown key" in {
      headers.getAll("z") must beEmpty
    }

    "should return all keys" in {
      headers.keys must be_==(Set("a", "b"))
    }

    "should return a simple map" in {
      headers.toSimpleMap must be_==(Map("a" -> "a1", "b" -> "b1"))
    }

    "return the value from a map by case insensitive" in {
      (headers.toMap.get("A") must_== Some(Seq("a1", "a2"))) and
        (headers.toMap.get("b") must_== Some(Seq("b1", "b2")))
    }

    "return the value from a simple map by case insensitive" in {
      (headers.toSimpleMap.get("A") must beSome("a1")) and
        (headers.toSimpleMap.get("b") must beSome("b1"))
    }
  }

  "Cookies" should {
    "merge two cookies" in {
      val cookies = Seq(
        Cookie("foo", "bar"),
        Cookie("bar", "qux"))

      Cookies.merge("", cookies) must ===("foo=bar; Path=/; HTTPOnly; bar=qux; Path=/; HTTPOnly")
    }
    "merge and remove duplicates" in {
      val cookies = Seq(
        Cookie("foo", "bar"),
        Cookie("foo", "baz"),
        Cookie("foo", "bar", domain = Some("Foo")),
        Cookie("foo", "baz", domain = Some("FoO")),
        Cookie("foo", "baz", secure = true),
        Cookie("foo", "baz", httpOnly = false),
        Cookie("foo", "bar", path = "/blah"),
        Cookie("foo", "baz", path = "/blah"))

      Cookies.merge("", cookies) must ===(
        "foo=baz; Path=/; Domain=FoO; HTTPOnly" + "; " + // Cookie("foo", "baz", domain=Some("FoO"))
          "foo=baz; Path=/" + "; " + // Cookie("foo", "baz", httpOnly=false)
          "foo=baz; Path=/blah; HTTPOnly" // Cookie("foo", "baz", path="/blah")
      )
    }
  }
}
