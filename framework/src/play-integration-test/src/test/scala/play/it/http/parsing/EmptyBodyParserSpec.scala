/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.parsing

import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Source
import play.api.test._
import play.api.mvc.BodyParsers

object EmptyBodyParserSpec extends PlaySpecification {

  "The empty body parser" should {

    def parse(bytes: Seq[Byte], contentType: Option[String], encoding: String)(implicit mat: FlowMaterializer) = {
      await(
        BodyParsers.parse.empty(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*))
          .run(Source.single(bytes.toArray))
      )
    }

    "parse empty bodies" in new WithApplication() {
      parse(Array[Byte](), Some("text/plain"), "utf-8") must beRight(())
    }

    "parse non-empty bodies" in new WithApplication() {
      parse(Array[Byte](1), Some("application/xml"), "utf-8") must beRight(())
      parse(Array[Byte](1, 2, 3), None, "utf-8") must beRight(())
    }

  }
}
