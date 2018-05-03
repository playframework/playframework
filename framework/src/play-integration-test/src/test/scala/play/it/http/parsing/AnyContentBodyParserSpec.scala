/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Application
import play.api.mvc._
import play.api.test._

class AnyContentBodyParserSpec extends PlaySpecification {

  "The anyContent body parser" should {
    def parse(method: String, contentType: Option[String], body: ByteString)(implicit app: Application) = {
      implicit val mat = app.materializer
      val parsers = app.injector.instanceOf[PlayBodyParsers]
      val request = FakeRequest(method, "/x").withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*)
      await(parsers.anyContent(request).run(Source.single(body)))
    }

    "parse text bodies for DELETE requests" in new WithApplication(_.globalApp(false)) {
      parse("DELETE", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
    }

    "parse text bodies for GET requests" in new WithApplication(_.globalApp(false)) {
      parse("GET", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
    }

    "parse empty bodies as raw for GET requests" in new WithApplication(_.globalApp(false)) {
      parse("PUT", None, ByteString.empty) must beRight.like {
        case AnyContentAsRaw(rawBuffer) => rawBuffer.asBytes() must beSome.like {
          case outBytes => outBytes must beEmpty
        }
      }
    }

    "parse text bodies for HEAD requests" in new WithApplication(_.globalApp(false)) {
      parse("HEAD", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
    }

    "parse text bodies for OPTIONS requests" in new WithApplication(_.globalApp(false)) {
      parse("OPTIONS", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
    }

    "parse XML bodies for PATCH requests" in new WithApplication(_.globalApp(false)) {
      parse("POST", Some("text/xml"), ByteString("<bar></bar>")) must beRight(AnyContentAsXml(<bar></bar>))
    }

    "parse text bodies for POST requests" in new WithApplication(_.globalApp(false)) {
      parse("POST", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
    }

    "parse JSON bodies for PUT requests" in new WithApplication(_.globalApp(false)) {
      parse("PUT", Some("application/json"), ByteString("""{"foo":"bar"}""")) must beRight.like {
        case AnyContentAsJson(json) => (json \ "foo").as[String] must_== "bar"
      }
    }

    "parse unknown bodies as raw for PUT requests" in new WithApplication(_.globalApp(false)) {
      parse("PUT", None, ByteString.empty) must beRight.like {
        case AnyContentAsRaw(rawBuffer) => rawBuffer.asBytes() must beSome.like {
          case outBytes => outBytes must beEmpty
        }
      }
    }

  }
}
