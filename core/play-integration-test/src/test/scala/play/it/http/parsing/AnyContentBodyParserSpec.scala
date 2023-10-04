/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.mvc._
import play.api.test._
import play.api.Application

class AnyContentBodyParserSpec extends PlaySpecification {
  "The anyContent body parser" should {
    def parse(method: String, contentType: Option[String], body: ByteString, maxLength: Option[Long] = None)(
        implicit app: Application
    ): Either[Result, AnyContent] = {
      implicit val mat = app.materializer
      val parsers      = app.injector.instanceOf[PlayBodyParsers]
      val request      = FakeRequest(method, "/x").withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*)
      await(parsers.anyContent(maxLength).apply(request).run(Source.single(body)))
    }

    "parse text bodies for DELETE requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("DELETE", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
      }
    }

    "parse text bodies for GET requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("GET", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
      }
    }

    "parse empty bodies as AnyContentAsEmpty for GET requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("GET", None, ByteString.empty) must beRight.like {
          case _: AnyContentAsEmpty.type => org.specs2.matcher.Matcher.success("", null) // does the job
        }
      }
    }

    "parse non empty bodies as raw for GET requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("GET", None, ByteString.fromString(" ")) must beRight.like {
          case AnyContentAsRaw(rawBuffer) =>
            rawBuffer.asBytes() must beSome[ByteString].like {
              case outBytes => outBytes must_== ByteString.fromString(" ")
            }
        }
      }
    }

    "parse text bodies for HEAD requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("HEAD", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
      }
    }

    "parse text bodies for OPTIONS requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("OPTIONS", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
      }
    }

    "parse XML bodies for PATCH requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("POST", Some("text/xml"), ByteString("<bar></bar>")) must beRight(AnyContentAsXml(<bar></bar>))
      }
    }

    "parse text bodies for POST requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("POST", Some("text/plain"), ByteString("bar")) must beRight(AnyContentAsText("bar"))
      }
    }

    "parse JSON bodies for PUT requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("PUT", Some("application/json"), ByteString("""{"foo":"bar"}""")) must beRight.like {
          case AnyContentAsJson(json) => (json \ "foo").as[String] must_== "bar"
        }
      }
    }

    "parse empty bodies as AnyContentAsEmpty for PUT requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("PUT", None, ByteString.empty) must beRight.like {
          case _: AnyContentAsEmpty.type => org.specs2.matcher.Matcher.success("", null) // does the job
        }
      }
    }

    "parse non empty bodies as raw for PUT requests" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("PUT", None, ByteString.fromString(" ")) must beRight.like {
          case AnyContentAsRaw(rawBuffer) =>
            rawBuffer.asBytes() must beSome[ByteString].like {
              case outBytes => outBytes must_== ByteString.fromString(" ")
            }
        }
      }
    }

    "accept greater than 2G bytes. not Int overflow" in new WithApplication(_.globalApp(false)) {
      override def running() = {
        parse("POST", Some("text/plain"), ByteString("bar"), maxLength = Some(Int.MaxValue.toLong + 2L)) must beRight(
          AnyContentAsText("bar")
        )
      }
    }
  }
}
