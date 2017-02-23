/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import org.specs2.mutable._

import play.core.test._

object HttpSpec extends Specification {
  "HTTP" title

  val headers = Headers("a" -> "a1", "a" -> "a2", "b" -> "b1", "b" -> "b2", "B" -> "b3", "c" -> "c1")

  "Headers" should {
    "return its headers as a sequence of name-value pairs" in {
      // Wrap sequence in a new Headers object so we can compare with Headers.equals
      new Headers(headers.headers) must_== headers
    }

    "return none for get(name) if no header present" in {
      headers.get("z") must beNone
    }

    "return first value for get(name) when headers are present" in {
      headers.get("a") must beSome("a1")
      headers.get("b") must beSome("b1")
      headers.get("c") must beSome("c1")
    }

    "throw exception for apply(name) if no header present" in {
      headers.apply("z") must throwA[Exception]
    }

    "return first value for apply(name) when headers are present" in {
      headers.apply("a") must_== "a1"
      headers.apply("b") must_== "b1"
      headers.apply("c") must_== "c1"
    }

    "return the header value associated with a by case insensitive" in {
      headers.get("a") must beSome("a1") and
        (headers.get("A") must beSome("a1"))
    }

    "return the header values associated with b by case insensitive" in {
      (headers.getAll("b") must_== Seq("b1", "b2", "b3")) and
        (headers.getAll("B") must_== Seq("b1", "b2", "b3"))
    }

    "not return an empty sequence of values associated with an unknown key" in {
      headers.getAll("z") must beEmpty
    }

    "should return all keys" in {
      headers.keys must be_==(Set("a", "b", "c"))
    }

    "should return a simple map" in {
      headers.toSimpleMap must be_==(Map("a" -> "a1", "b" -> "b1", "c" -> "c1"))
    }

    "return the value from a map by case insensitive" in {
      (headers.toMap.get("A") must_== Some(Seq("a1", "a2"))) and
        (headers.toMap.get("b") must_== Some(Seq("b1", "b2", "b3")))
    }

    "return the value from a simple map by case insensitive" in {
      (headers.toSimpleMap.get("A") must beSome("a1")) and
        (headers.toSimpleMap.get("b") must beSome("b1"))
    }

    "add headers" in {
      headers.add("a" -> "a3", "a" -> "a4").getAll("a") must_== Seq("a1", "a2", "a3", "a4")
    }

    "remove headers by case insensitive" in {
      headers.remove("a").getAll("a") must beEmpty and
        (headers.remove("A").getAll("a") must beEmpty)
    }

    "replace headers by case insensitive" in {
      headers.replace("a" -> "a3", "A" -> "a4").getAll("a") must_== Seq("a3", "a4")
    }

    "equal other Headers by case insensitive" in {
      val other = Headers("A" -> "a1", "a" -> "a2", "b" -> "b1", "b" -> "b2", "B" -> "b3", "C" -> "c1")
      (headers must_== other) and
        (headers.## must_== other.##)
    }

    "equal other Headers with same relative order" in {
      val other = Headers("A" -> "a1", "a" -> "a2", "b" -> "b1", "b" -> "b2", "B" -> "b3", "c" -> "c1")
      (headers must_== other) and
        (headers.## must_== other.##)
    }

    "not equal other Headers with different relative order" in {
      headers must_!= Headers("a" -> "a2", "A" -> "a1", "b" -> "b1", "b" -> "b2", "B" -> "b3", "c" -> "C1")
    }
  }

  "Cookies" should {
    "merge two cookies" in withApplication {
      val cookies = Seq(
        Cookie("foo", "bar"),
        Cookie("bar", "qux"))

      Cookies.mergeSetCookieHeader("", cookies) must ===("foo=bar; Path=/; HTTPOnly;;bar=qux; Path=/; HTTPOnly")
    }
    "merge and remove duplicates" in withApplication {
      val cookies = Seq(
        Cookie("foo", "bar"),
        Cookie("foo", "baz"),
        Cookie("foo", "bar", domain = Some("Foo")),
        Cookie("foo", "baz", domain = Some("FoO")),
        Cookie("foo", "baz", secure = true),
        Cookie("foo", "baz", httpOnly = false),
        Cookie("foo", "bar", path = "/blah"),
        Cookie("foo", "baz", path = "/blah"))

      Cookies.mergeSetCookieHeader("", cookies) must ===(
        "foo=baz; Path=/; Domain=FoO; HTTPOnly" + ";;" + // Cookie("foo", "baz", domain=Some("FoO"))
          "foo=baz; Path=/" + ";;" + // Cookie("foo", "baz", httpOnly=false)
          "foo=baz; Path=/blah; HTTPOnly" // Cookie("foo", "baz", path="/blah")
      )
    }
  }
}
