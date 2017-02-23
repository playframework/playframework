/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http.parsing

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc._
import play.api.test._

object DefaultBodyParserSpec extends PlaySpecification {

  "The default body parser" should {

    def parse(method: String, contentType: Option[String], body: ByteString)(implicit mat: Materializer) = {
      val request = FakeRequest(method, "/x").withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*)
      await(BodyParsers.parse.default(request).run(Source.single(body)))
    }

    "ignore text bodies for DELETE requests" in new WithApplication() {
      parse("GET", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsEmpty)
    }

    "ignore text bodies for GET requests" in new WithApplication() {
      parse("GET", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsEmpty)
    }

    "ignore text bodies for HEAD requests" in new WithApplication() {
      parse("HEAD", None, ByteString("bar")) must beRight(AnyContentAsEmpty)
    }

    "ignore text bodies for OPTIONS requests" in new WithApplication() {
      parse("GET", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsEmpty)
    }

    "parse XML bodies for PATCH requests" in new WithApplication() {
      parse("POST", Some("text/xml"), ByteString("<bar></bar>")) must beRight(AnyContentAsXml(<bar></bar>))
    }

    "parse text bodies for POST requests" in new WithApplication() {
      parse("POST", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
    }

    "parse JSON bodies for PUT requests" in new WithApplication() {
      parse("PUT", Some("application/json"), ByteString("""{"foo":"bar"}""")) must beRight.like {
        case AnyContentAsJson(json) => (json \ "foo").as[String] must_== "bar"
      }
    }

    "parse unknown bodies as raw for PUT requests" in new WithApplication() {
      parse("PUT", None, ByteString.empty) must beRight.like {
        case AnyContentAsRaw(rawBuffer) => rawBuffer.asBytes() must beSome.like {
          case outBytes => outBytes must beEmpty
        }
      }
    }

  }
}
