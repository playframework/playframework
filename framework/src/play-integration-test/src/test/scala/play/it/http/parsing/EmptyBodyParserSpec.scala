/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.parsing

import play.api.test._
import play.api.mvc.{ BodyParser, BodyParsers }
import play.api.libs.iteratee.Enumerator

object EmptyBodyParserSpec extends PlaySpecification {

  "The empty body parser" should {

    def parse(bytes: Seq[Byte], contentType: Option[String], encoding: String) = {
      await(Enumerator(bytes.to[Array]) |>>>
        BodyParsers.parse.empty(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*)))
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
