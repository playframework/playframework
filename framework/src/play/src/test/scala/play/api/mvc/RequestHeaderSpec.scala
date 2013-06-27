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
        accept("en") must contain(Lang("en")).only
      }

      "parse a single accept language and country" in {
        accept("en-US") must contain(Lang("en", "US")).only
      }

      "parse multiple accept languages" in {
        accept("en-US, es") must contain(Lang("en", "US"), Lang("es")).inOrder.only
      }

      "sort accept languages by quality" in {
        accept("en-US;q=0.8, es;q=0.7") must contain(Lang("en", "US"), Lang("es")).inOrder.only
        accept("en-US;q=0.7, es;q=0.8") must contain(Lang("es"), Lang("en", "US")).inOrder.only
      }

      "default accept language quality to 1" in {
        accept("en-US, es;q=0.7") must contain(Lang("en", "US"), Lang("es")).inOrder.only
        accept("en-US;q=0.7, es") must contain(Lang("es"), Lang("en", "US")).inOrder.only
      }

    }
  }

  def accept(value: String) = DummyRequestHeader(Map("Accept-Language" -> Seq(value))).acceptLanguages

  case class DummyRequestHeader(headersMap: Map[String, Seq[String]] = Map()) extends RequestHeader{
    def id = 1
    def tags = Map()
    def uri = ""
    def path = ""
    def method = ""
    def version = ""
    def queryString = Map()
    def remoteAddress = ""
    lazy val headers = new Headers { val data = headersMap.toSeq }
  }
}
