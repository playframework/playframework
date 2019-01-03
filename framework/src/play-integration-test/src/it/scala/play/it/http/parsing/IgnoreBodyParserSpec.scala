/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.test._
import play.api.mvc.BodyParsers

class IgnoreBodyParserSpec extends PlaySpecification {

  "The ignore body parser" should {

    def parse[A](value: A, bytes: ByteString, contentType: Option[String], encoding: String)(implicit mat: Materializer) = {
      await(
        BodyParsers.utils.ignore(value)(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*))
          .run(Source.single(bytes))
      )
    }

    "ignore empty bodies" in new WithApplication() {
      parse("foo", ByteString.empty, Some("text/plain"), "utf-8") must beRight("foo")
    }

    "ignore non-empty bodies" in new WithApplication() {
      parse(42, ByteString(1), Some("application/xml"), "utf-8") must beRight(42)
      parse("foo", ByteString(1, 2, 3), None, "utf-8") must beRight("foo")
    }

  }
}
