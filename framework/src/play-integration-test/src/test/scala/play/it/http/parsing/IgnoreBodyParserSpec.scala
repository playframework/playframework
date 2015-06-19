/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.parsing

import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Source
import play.api.test._
import play.api.mvc.BodyParsers

object IgnoreBodyParserSpec extends PlaySpecification {

  "The ignore body parser" should {

    def parse[A](value: A, bytes: Seq[Byte], contentType: Option[String], encoding: String)(implicit mat: FlowMaterializer) = {
      await(
        BodyParsers.parse.ignore(value)(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*))
          .run(Source.single(bytes.to[Array]))
      )
    }

    "ignore empty bodies" in new WithApplication() {
      parse("foo", Array[Byte](), Some("text/plain"), "utf-8") must beRight("foo")
    }

    "ignore non-empty bodies" in new WithApplication() {
      parse(42, Array[Byte](1), Some("application/xml"), "utf-8") must beRight(42)
      parse("foo", Array[Byte](1, 2, 3), None, "utf-8") must beRight("foo")
    }

  }
}
