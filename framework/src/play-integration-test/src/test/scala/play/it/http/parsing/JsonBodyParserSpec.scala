package play.it.http.parsing

import play.api.test._
import play.api.mvc.{BodyParser, BodyParsers}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue

object JsonBodyParserSpec extends PlaySpecification {

  "The JSON body parser" should {

    def parse(json: String, contentType: Option[String], encoding: String, bodyParser: BodyParser[JsValue] = BodyParsers.parse.tolerantJson) = {
      await(Enumerator(json.getBytes(encoding)) |>>>
        bodyParser(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq:_*)))
    }

    "parse JSON bodies" in new WithApplication() {
      parse("""{"foo":"bar"}""", Some("application/json"), "utf-8") must beRight.like {
        case json => (json \ "foo").as[String] must_== "bar"
      }
    }

    "automatically detect the charset" in new WithApplication() {
      parse("""{"foo":"bär"}""", Some("application/json"), "utf-8") must beRight.like {
        case json => (json \ "foo").as[String] must_== "bär"
      }
      parse("""{"foo":"bär"}""", Some("application/json"), "utf-16") must beRight.like {
        case json => (json \ "foo").as[String] must_== "bär"
      }
      parse("""{"foo":"bär"}""", Some("application/json"), "utf-32") must beRight.like {
        case json => (json \ "foo").as[String] must_== "bär"
      }
    }

    "ignore the supplied charset" in new WithApplication() {
      parse("""{"foo":"bär"}""", Some("application/json; charset=iso-8859-1"), "utf-16") must beRight.like {
        case json => (json \ "foo").as[String] must_== "bär"
      }
    }

    "accept all common json content types" in new WithApplication() {
      parse("""{"foo":"bar"}""", Some("application/json"), "utf-8", BodyParsers.parse.json) must beRight.like {
        case json => (json \ "foo").as[String] must_== "bar"
      }
      parse("""{"foo":"bar"}""", Some("text/json"), "utf-8", BodyParsers.parse.json) must beRight.like {
        case json => (json \ "foo").as[String] must_== "bar"
      }
    }

    "reject non json content types" in new WithApplication() {
      parse("""{"foo":"bar"}""", Some("application/xml"), "utf-8", BodyParsers.parse.json) must beLeft
      parse("""{"foo":"bar"}""", None, "utf-8", BodyParsers.parse.json) must beLeft
    }

    "gracefully handle invalid json" in new WithApplication() {
      parse("""{"foo:}""", Some("application/json"), "utf-8", BodyParsers.parse.json) must beLeft
    }

  }

}
