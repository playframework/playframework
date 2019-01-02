/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Application
import play.api.test._
import play.api.mvc.{ BodyParser, PlayBodyParsers }

class EmptyBodyParserSpec extends PlaySpecification {

  "The empty body parser" should {

    implicit def emptyBodyParser(implicit app: Application) = app.injector.instanceOf[PlayBodyParsers].empty

    def parse(bytes: ByteString, contentType: Option[String], encoding: String)(implicit mat: Materializer, bodyParser: BodyParser[Unit]) = {
      await(
        bodyParser(
          FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*)
        ).run(Source.single(bytes))
      )
    }

    "parse empty bodies" in new WithApplication() {
      parse(ByteString.empty, Some("text/plain"), "utf-8") must beRight(())
    }

    "parse non-empty bodies" in new WithApplication() {
      parse(ByteString(1), Some("application/xml"), "utf-8") must beRight(())
      parse(ByteString(1, 2, 3), None, "utf-8") must beRight(())
    }

  }
}
