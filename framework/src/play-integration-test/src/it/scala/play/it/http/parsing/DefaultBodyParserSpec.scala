/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Application
import play.api.mvc._
import play.api.test._

class DefaultBodyParserSpec extends PlaySpecification {

  "The default body parser" should {

    implicit def defaultBodyParser(implicit app: Application) = app.injector.instanceOf[PlayBodyParsers].default

    def parse(method: String, contentType: Option[String], body: ByteString)(implicit mat: Materializer, defaultBodyParser: BodyParser[AnyContent]) = {
      val request = FakeRequest(method, "/x").withHeaders(
        contentType.map(CONTENT_TYPE -> _).toSeq :+ (CONTENT_LENGTH -> body.length.toString): _*)
      await(defaultBodyParser(request).run(Source.single(body)))
    }

    "parse text bodies for DELETE requests" in new WithApplication() {
      parse("GET", Some("text/plain"), ByteString("bar")) must be right (AnyContentAsText("bar"))
    }

    "parse text bodies for GET requests" in new WithApplication() {
      parse("GET", Some("text/plain"), ByteString("bar")) must be right (AnyContentAsText("bar"))
    }

    "parse text bodies for HEAD requests" in new WithApplication() {
      parse("HEAD", Some("text/plain"), ByteString("bar")) must be right (AnyContentAsText("bar"))
    }

    "parse text bodies for OPTIONS requests" in new WithApplication() {
      parse("GET", Some("text/plain"), ByteString("bar")) must be right (AnyContentAsText("bar"))
    }

    "parse XML bodies for PATCH requests" in new WithApplication() {
      parse("POST", Some("text/xml"), ByteString("<bar></bar>")) must be right (AnyContentAsXml(<bar></bar>))
    }

    "parse text bodies for POST requests" in new WithApplication() {
      parse("POST", Some("text/plain"), ByteString("bar")) must be right (AnyContentAsText("bar"))
    }

    "parse JSON bodies for PUT requests" in new WithApplication() {
      parse("PUT", Some("application/json"), ByteString("""{"foo":"bar"}""")) must beRight.like {
        case AnyContentAsJson(json) => (json \ "foo").as[String] must_== "bar"
      }
    }

    "parse unknown empty bodies as empty for PUT requests" in new WithApplication() {
      parse("PUT", None, ByteString.empty) must be right (AnyContentAsEmpty)
    }

    "parse unknown bodies as raw for PUT requests" in new WithApplication() {
      parse("PUT", None, ByteString("abc")) must beRight.like {
        case AnyContentAsRaw(rawBuffer) => rawBuffer.asBytes() must beSome.like {
          case outBytes => outBytes must_== ByteString("abc")
        }
      }
    }

  }
}
