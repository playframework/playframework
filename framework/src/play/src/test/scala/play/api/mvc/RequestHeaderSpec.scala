/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import org.specs2.mutable.Specification
import play.api.i18n.Lang

class RequestHeaderSpec extends Specification {

  "request header" should {

    "parse accept languages" in {

      "return an empty sequence when no accept languages specified" in {
        DummyRequestHeader().acceptLanguages must beEmpty
      }

      "parse a single accept language" in {
        accept("en") must contain(exactly(Lang("en")))
      }

      "parse a single accept language and country" in {
        accept("en-US") must contain(exactly(Lang("en", "US")))
      }

      "parse multiple accept languages" in {
        accept("en-US, es") must contain(exactly(Lang("en", "US"), Lang("es")).inOrder)
      }

      "sort accept languages by quality" in {
        accept("en-US;q=0.8, es;q=0.7") must contain(exactly(Lang("en", "US"), Lang("es")).inOrder)
        accept("en-US;q=0.7, es;q=0.8") must contain(exactly(Lang("es"), Lang("en", "US")).inOrder)
      }

      "default accept language quality to 1" in {
        accept("en-US, es;q=0.7") must contain(exactly(Lang("en", "US"), Lang("es")).inOrder)
        accept("en-US;q=0.7, es") must contain(exactly(Lang("es"), Lang("en", "US")).inOrder)
      }

    }
  }

  def accept(value: String) = DummyRequestHeader(Headers("Accept-Language" -> value)).acceptLanguages

  case class DummyRequestHeader(headers: Headers = Headers()) extends RequestHeader {
    def id = 1
    def tags = Map()
    def uri = ""
    def path = ""
    def method = ""
    def version = ""
    def queryString = Map()
    def remoteAddress = ""
    def secure = false
  }
}
