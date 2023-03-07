/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import akka.stream.scaladsl.Source
import akka.stream.Materializer
import akka.util.ByteString
import play.api.mvc.BodyParser
import play.api.mvc.PlayBodyParsers
import play.api.test._
import play.api.Application

class TextBodyParserSpec extends PlaySpecification {
  implicit def tolerantTextBodyParser(implicit app: Application): BodyParser[String] =
    app.injector.instanceOf[PlayBodyParsers].tolerantText

  "The text body parser" should {
    def parse(text: String, contentType: Option[String], encoding: String)(
        implicit mat: Materializer,
        bodyParser: BodyParser[String]
    ) = {
      await(
        bodyParser(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*))
          .run(Source.single(ByteString(text, encoding)))
      )
    }

    "parse text bodies" in new WithApplication() {
      parse("bar", Some("text/plain"), "utf-8") must beRight("bar")
    }

    "honour the declared charset" in new WithApplication() {
      parse("bär", Some("text/plain; charset=utf-8"), "utf-8") must beRight("bär")
      parse("bär", Some("text/plain; charset=utf-16"), "utf-16") must beRight("bär")
      parse("bär", Some("text/plain; charset=iso-8859-1"), "iso-8859-1") must beRight("bär")
    }

    "default to us-ascii encoding" in new WithApplication() {
      parse("bär", Some("text/plain"), "us-ascii") must beRight("b?r")
      parse("bär", None, "us-ascii") must beRight("b?r")
      parse("bär", None, "us-ascii") must beRight("b?r")
    }

    "accept text/plain content type" in new WithApplication() {
      parse("bar", Some("text/plain"), "utf-8") must beRight("bar")
    }

    "reject non text/plain content types" in new WithApplication() {
      val textBodyParser = app.injector.instanceOf[PlayBodyParsers].text
      parse("bar", Some("application/xml"), "utf-8")(app.materializer, textBodyParser) must beLeft
      parse("bar", None, "utf-8")(app.materializer, textBodyParser) must beLeft
    }
  }
}
