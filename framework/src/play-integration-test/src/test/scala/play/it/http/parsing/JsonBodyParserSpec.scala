/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.parsing

import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{ Json, JsError }
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ BodyParser, BodyParsers }
import play.api.test._

object JsonBodyParserSpec extends PlaySpecification {

  private case class Foo(a: Int, b: String)
  private implicit val fooFormat = Json.format[Foo]

  "The JSON body parser" should {

    def parse[A](json: String, contentType: Option[String], encoding: String, bodyParser: BodyParser[A] = BodyParsers.parse.tolerantJson) = {
      await(Enumerator(json.getBytes(encoding)) |>>>
        bodyParser(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*)))
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

    "validate json content using .validate" in new WithApplication() {
      import scala.concurrent.ExecutionContext.Implicits.global

      val fooParser = BodyParsers.parse.json.validate {
        _.validate[Foo].asEither.left.map(e => BadRequest(JsError.toFlatJson(e)))
      }
      parse("""{"a":1,"b":"bar"}""", Some("application/json"), "utf-8", fooParser) must beRight
      parse("""{"foo":"bar"}""", Some("application/json"), "utf-8", fooParser) must beLeft
      parse("""{"a":1}""", Some("application/json"), "utf-8", fooParser) must beLeft
    }

    "validate json content using implicit reads" in new WithApplication() {
      parse("""{"a":1,"b":"bar"}""", Some("application/json"), "utf-8", BodyParsers.parse.json[Foo]) must beRight.like {
        case foo => foo must_== Foo(1, "bar")
      }
      parse("""{"foo":"bar"}""", Some("application/json"), "utf-8", BodyParsers.parse.json[Foo]) must beLeft
      parse("""{"a":1}""", Some("application/json"), "utf-8", BodyParsers.parse.json[Foo]) must beLeft
      parse("""{"foo:}""", Some("application/json"), "utf-8", BodyParsers.parse.json[Foo]) must beLeft
    }

  }

}
