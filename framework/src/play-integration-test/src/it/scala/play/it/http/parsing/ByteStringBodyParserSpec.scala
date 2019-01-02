/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.PlayBodyParsers
import play.api.test._

class ByteStringBodyParserSpec extends PlaySpecification {

  "The ByteString body parser" should {

    def parsers(implicit mat: Materializer) = PlayBodyParsers()
    def parser(implicit mat: Materializer) = parsers.byteString.apply(FakeRequest())

    "parse single byte string bodies" in new WithApplication() {
      await(parser.run(ByteString("bar"))) must beRight(ByteString("bar"))
    }

    "parse multiple chunk byte string bodies" in new WithApplication() {
      await(parser.run(
        Source(List(ByteString("foo"), ByteString("bar")))
      )) must beRight(ByteString("foobar"))
    }

    "refuse to parse bodies greater than max length" in new WithApplication() {
      val parser = parsers.byteString(4).apply(FakeRequest())
      await(parser.run(
        Source(List(ByteString("foo"), ByteString("bar")))
      )) must beLeft
    }
  }
}
