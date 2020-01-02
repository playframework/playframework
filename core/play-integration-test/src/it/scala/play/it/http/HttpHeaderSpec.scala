/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.mvc.Cookie
import play.api.mvc.DefaultCookieHeaderEncoding
import play.core.test._

class HttpHeaderSpec extends HttpHeadersCommonSpec {
  "HTTP" title

  "Headers should" in {
    commonTests()
  }

  "Cookies" should {
    lazy val cookieHeaderEncoding = new DefaultCookieHeaderEncoding()

    "merge two cookies" in withApplication {
      val cookies = Seq(Cookie("foo", "bar"), Cookie("bar", "qux"))

      cookieHeaderEncoding.mergeSetCookieHeader("", cookies) must ===(
        "foo=bar; Path=/; HTTPOnly;;bar=qux; Path=/; HTTPOnly"
      )
    }
    "merge and remove duplicates" in withApplication {
      val cookies = Seq(
        Cookie("foo", "bar"),
        Cookie("foo", "baz"),
        Cookie("foo", "baz", secure = true),
        Cookie("foo", "baz", httpOnly = false),
        Cookie("foo", "bar", domain = Some("Foo")),
        Cookie("foo", "baz", domain = Some("FoO")),
        Cookie("foo", "bar", path = "/blah"),
        Cookie("foo", "baz", path = "/blah")
      )

      cookieHeaderEncoding.mergeSetCookieHeader("", cookies) must ===(
        "foo=baz; Path=/" + ";;" +                         // Cookie("foo", "baz", httpOnly=false)
          "foo=baz; Path=/; Domain=FoO; HTTPOnly" + ";;" + // Cookie("foo", "baz", domain=Some("FoO"))
          "foo=baz; Path=/blah; HTTPOnly"                  // Cookie("foo", "baz", path="/blah")
      )
    }
  }
}
