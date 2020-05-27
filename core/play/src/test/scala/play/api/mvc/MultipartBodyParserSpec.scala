/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl.TestSource
import akka.testkit.TestKit
import akka.util.ByteString
import org.specs2.mutable.SpecificationLike
import play.core.parsers.Multipart
import play.core.test.{FakeHeaders, FakeRequest}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MultipartBodyParserSpec extends TestKit(ActorSystem("daf")) with SpecificationLike {
  "Multipart body parser" should {
    implicit val system           = ActorSystem()
    implicit val materializer     = Materializer.matFromSystem
    implicit val executionContext = system.dispatcher

    val playBodyParsers = PlayBodyParsers(tfc = new InMemoryTemporaryFileCreator(10))

    "return an error if temporary file creation fails" in {
      val fileSize = 100
      val boundary = "-----------------------------14568445977970839651285587160"
      val header =
        s"--$boundary\r\n" +
          "Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"uploadedfile.txt\"\r\n" +
          "Content-Type: application/octet-stream\r\n" +
          "\r\n"
      val content = Array.ofDim[Byte](fileSize)
      val footer =
        "\r\n" +
          "\r\n" +
          s"--$boundary--\r\n"

      val body = Source(
        ByteString(header) ::
          ByteString(content) ::
          ByteString(footer) ::
          Nil
      )

      val bodySize = header.length + fileSize + footer.length

      val request = FakeRequest(
        method = "POST",
        uri = "/x",
        headers = FakeHeaders(
          Seq("Content-Type" -> s"multipart/form-data; boundary=$boundary", "Content-Length" -> bodySize.toString)
        ),
        body = body
      )

      val response = playBodyParsers.multipartFormData.apply(request).run(body)
      Await.result(response, Duration.Inf) must throwA[IOOperationIncompleteException]
    }

    "publisher" in {
      val boundary = "aabbccddee"
      val bodyStr =
        s"""
           |--aabbccddee
           |Content-Disposition: form-data; name="file1"; filename="file1.txt"
           |Content-Type: text/plain
           |
           |the first file
           |--aabbccddee
           |Content-Disposition: form-data; name="file2"; filename="file2.txt"
           |Content-Type: text/plain
           |
           |the second file
           |--aabbccddee--
           |""".stripMargin.linesIterator.mkString("\r\n")
      val request = FakeRequest(
        method = "POST",
        uri = "/x",
        headers = FakeHeaders(
          Seq("Content-Type" -> s"multipart/form-data; boundary=$boundary")
        ),
        body = bodyStr
      )
      val testSource = TestSource.probe[ByteString]
      val (publisher, res) = testSource.toMat(
        playBodyParsers.multipartFormData(Multipart.handleFilePartAsStream)(request)
          .toSink
      )(Keep.both).run()

      publisher
          .sendNext(ByteString(request.body))
          .sendComplete()

      Await.result(res, Duration.Inf) must beRight[MultipartFormData[Source[ByteString, NotUsed]]].like {
        case parts =>
          val strSink = Sink.fold[String, ByteString]("")((l, r) => l + r.utf8String)
          val file1Content = Await.result(parts.file("file1").get.ref.runWith(strSink), Duration.Inf)
          val file2Content = Await.result(parts.file("file2").get.ref.runWith(strSink), Duration.Inf)
          file1Content must equalTo("the first file")
          file2Content must equalTo("the second file")
      }
    }
  }
}
