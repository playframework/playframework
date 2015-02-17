/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.parsing

import play.api.test._
import play.api.mvc.BodyParsers
import play.api.libs.iteratee.Enumerator

object IgnoreBodyParserSpec extends PlaySpecification {

  "The ignore body parser" should {

    def parse[A](value: A, bytes: Seq[Byte], contentType: Option[String], encoding: String) = {
      await(Enumerator(bytes.to[Array]) |>>>
        BodyParsers.parse.ignore(value)(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*)))
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
