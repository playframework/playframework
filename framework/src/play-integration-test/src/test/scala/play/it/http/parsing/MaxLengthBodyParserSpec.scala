/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.parsing

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.{ MaxSizeExceeded, BodyParsers }
import play.api.test._

object MaxLengthBodyParserSpec extends PlaySpecification {

  "The maxSize body parser" should {

    def parse(size: Long, underlyingSize: Long, bytes: ByteString, contentType: Option[String], encoding: String)(implicit mat: Materializer) = {
      val request = FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*)
      val parser = BodyParsers.parse.maxLength(size, BodyParsers.parse.anyContent(Some(underlyingSize)))
      await(parser(request).run(Source.single(bytes)))
    }

    "return MaxSizeExceeded with a limit lower than the underlying parser" in new WithApplication() {
      parse(1, 10, ByteString("foo"), Some("text/plain"), "utf-8") must beRight.like {
        case Left(MaxSizeExceeded(size)) => size must_== 1
      }
    }

    "return EntityTooLarge with a limit equal to the underlying parser" in new WithApplication() {
      parse(1, 1, ByteString("foo"), Some("text/plain"), "utf-8") must beLeft.like {
        case r => r.header.status == REQUEST_ENTITY_TOO_LARGE
      }
    }
  }
}
