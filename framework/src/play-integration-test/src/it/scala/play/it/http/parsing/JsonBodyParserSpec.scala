/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Application
import play.api.libs.json.{ JsError, JsValue, Json }
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ BodyParser, PlayBodyParsers }
import play.api.test._

class JsonBodyParserSpec extends PlaySpecification {

  private case class Foo(a: Int, b: String)
  private implicit val fooFormat = Json.format[Foo]

  implicit def tolerantJsonBodyParser(implicit app: Application) = app.injector.instanceOf[PlayBodyParsers].tolerantJson

  def jsonBodyParser(implicit app: Application) = app.injector.instanceOf[PlayBodyParsers].json

  "The JSON body parser" should {

    def parse[A](json: String, contentType: Option[String], encoding: String)(implicit mat: Materializer, bodyParser: BodyParser[A]) = {
      await(
        bodyParser(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*))
          .run(Source.single(ByteString(json.getBytes(encoding))))
      )
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
      parse("""{"foo":"bar"}""", Some("application/json"), "utf-8") must beRight.like {
        case json => (json \ "foo").as[String] must_== "bar"
      }
      parse("""{"foo":"bar"}""", Some("text/json"), "utf-8") must beRight.like {
        case json => (json \ "foo").as[String] must_== "bar"
      }
    }

    "reject non json content types" in new WithApplication() {
      parse("""{"foo":"bar"}""", Some("application/xml"), "utf-8")(app.materializer, jsonBodyParser) must beLeft
      parse("""{"foo":"bar"}""", None, "utf-8")(app.materializer, jsonBodyParser) must beLeft
    }

    "gracefully handle invalid json" in new WithApplication() {
      parse("""{"foo:}""", Some("application/json"), "utf-8") must beLeft
    }

    "validate json content using .validate" in new WithApplication() {
      import scala.concurrent.ExecutionContext.Implicits.global

      val fooParser: BodyParser[Foo] = jsonBodyParser.validate {
        _.validate[Foo].asEither.left.map(e => BadRequest(JsError.toJson(e)))
      }
      parse("""{"a":1,"b":"bar"}""", Some("application/json"), "utf-8")(app.materializer, fooParser) must beRight
      parse("""{"foo":"bar"}""", Some("application/json"), "utf-8")(app.materializer, fooParser) must beLeft
      parse("""{"a":1}""", Some("application/json"), "utf-8")(app.materializer, fooParser) must beLeft
    }

    "validate json content using implicit reads" in new WithApplication() {

      val parser = app.injector.instanceOf[PlayBodyParsers].json[Foo]

      parse("""{"a":1,"b":"bar"}""", Some("application/json"), "utf-8")(app.materializer, parser) must beRight.like {
        case foo => foo must_== Foo(1, "bar")
      }
      parse("""{"foo":"bar"}""", Some("application/json"), "utf-8")(app.materializer, parser) must beLeft
      parse("""{"a":1}""", Some("application/json"), "utf-8")(app.materializer, parser) must beLeft
      parse("""{"foo:}""", Some("application/json"), "utf-8")(app.materializer, parser) must beLeft
    }

  }

}
