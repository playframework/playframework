/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.net.URI

import org.specs2.mutable.Specification
import play.api.http.HeaderNames._
import play.api.i18n.Lang
import play.api.libs.prop.{ Prop, PropMap }

class RequestHeaderSpec extends Specification {

  def newRequestHeader(ps: Prop.WithValue[_]*): RequestHeader = new RequestHeaderImpl(RequestHeader.defaultBehavior, PropMap(ps: _*))

  "request header" should {

    "have default values for its properties" in {
      "headers should be present and empty" in {
        val rh = newRequestHeader()
        rh.containsProp(RequestHeaderProp.Headers) must beTrue
        val headers: Headers = rh.prop(RequestHeaderProp.Headers)
        headers must not(beNull)
        headers.toMap must beEmpty
      }
    }

    "allow some properties to be accessed through helpers" in {
      "headers" in {
        val rh = newRequestHeader(RequestHeaderProp.Headers ~> Headers("X" -> "y"))
        rh.headers must_== Headers("X" -> "y")
      }
    }

    "support the copy method" in {
      "for copying headers" in {
        val rh = newRequestHeader()
        rh.copy(headers = Headers("A" -> "b")).headers must_== Headers("A" -> "b")
      }
    }

    "handle host" in {
      "relative uri with host header" in {
        val rh = newRequestHeader(
          RequestHeaderProp.Uri ~> "/",
          RequestHeaderProp.Headers ~> Headers(HOST -> "playframework.com")
        )
        rh.host must_== "playframework.com"
      }
      "absolute uri" in {
        val rh = newRequestHeader(
          RequestHeaderProp.Uri ~> "https://example.com/test",
          RequestHeaderProp.Headers ~> Headers(HOST -> "playframework.com")
        )
        rh.host must_== "example.com"
      }
      "absolute uri with port" in {
        val rh = newRequestHeader(
          RequestHeaderProp.Uri ~> "https://example.com:8080/test",
          RequestHeaderProp.Headers ~> Headers(HOST -> "playframework.com")
        )
        rh.host must_== "example.com:8080"
      }
      "absolute uri with port and invalid characters" in {
        val rh = newRequestHeader(
          RequestHeaderProp.Uri ~> "https://example.com:8080/classified-search/classifieds?version=GTI|V8",
          RequestHeaderProp.Headers ~> Headers(HOST -> "playframework.com")
        )
        rh.host must_== "example.com:8080"
      }
      "relative uri with invalid characters" in {
        val rh = newRequestHeader(
          RequestHeaderProp.Uri ~> "/classified-search/classifieds?version=GTI|V8",
          RequestHeaderProp.Headers ~> Headers(HOST -> "playframework.com")
        )
        rh.host must_== "playframework.com"
      }
    }

    "parse accept languages" in {

      "return an empty sequence when no accept languages specified" in {
        // FIXME: Handle missing prop
        newRequestHeader(RequestHeaderProp.Headers ~> Headers()).acceptLanguages must beEmpty
      }

      "parse a single accept language" in {
        accept("en") must contain(exactly(Lang("en")))
      }

      "parse a single accept language and country" in {
        accept("en-US") must contain(exactly(Lang("en-US")))
      }

      "parse multiple accept languages" in {
        accept("en-US, es") must contain(exactly(Lang("en-US"), Lang("es")).inOrder)
      }

      "sort accept languages by quality" in {
        accept("en-US;q=0.8, es;q=0.7") must contain(exactly(Lang("en-US"), Lang("es")).inOrder)
        accept("en-US;q=0.7, es;q=0.8") must contain(exactly(Lang("es"), Lang("en-US")).inOrder)
      }

      "default accept language quality to 1" in {
        accept("en-US, es;q=0.7") must contain(exactly(Lang("en-US"), Lang("es")).inOrder)
        accept("en-US;q=0.7, es") must contain(exactly(Lang("es"), Lang("en-US")).inOrder)
      }

    }
  }

  def accept(value: String) = newRequestHeader(
    RequestHeaderProp.Headers ~> Headers("Accept-Language" -> value)
  ).acceptLanguages

}
