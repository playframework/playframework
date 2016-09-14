/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.formatters

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.api.mvc.MultipartFormData.SourcePart
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Await
import scala.concurrent.duration._

class MultipartSpec extends Specification with AfterAll {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()(system)

  def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
  }

  val reduce = Sink.reduce[ByteString]((v1, v2) => v1 ++ v2)

  def source(contentId: Option[String] = None, transferEncoding: Option[String] = None,
    contentType: Option[String] = None, charset: Option[String] = None) = {
    val data = Source.single(ByteString.fromString("world"))
    Source.single(SourcePart("hello", data, None, contentType, charset, transferEncoding, contentId))
  }

  "Multipart.transform" should {

    "generate a Content-ID" in {
      val res = Await.result(Multipart.transform(source(Option("123")), "play")
        .runWith(reduce).map(_.utf8String), 10.seconds)

      res must contain("Content-ID: 123")
    }

    "generate a Content-Transfer-Encoding" in {
      val res = Await.result(Multipart.transform(source(None, Option("8-bit")), "play")
        .runWith(reduce).map(_.utf8String), 10.seconds)

      res must contain("Content-Transfer-Encoding: 8-bit")
    }

    "generate a Content-Type with charset" in {
      val res = Await.result(Multipart.transform(source(None, None, Option("text/plain"), Option("UTF-8")), "play")
        .runWith(reduce).map(_.utf8String), 10.seconds)

      res must contain("Content-Type: text/plain; charset=UTF-8")
    }

  }

}
