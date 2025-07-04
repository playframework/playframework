/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream._
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.util.ByteString
import org.specs2.mutable.Specification
import play.core.test.FakeHeaders
import play.core.test.FakeRequest

class MultipartBodyParserSpec extends Specification {
  "Multipart body parser" should {
    implicit val system: ActorSystem                = ActorSystem()
    implicit val materializer: Materializer         = Materializer.matFromSystem
    implicit val executionContext: ExecutionContext = system.dispatcher

    val playBodyParsers = PlayBodyParsers(tfc = new InMemoryTemporaryFileCreator(10))

    "return an error if temporary file creation fails" in {
      val fileSize = 100
      val boundary = "-----------------------------14568445977970839651285587160"
      val header   =
        s"--$boundary\r\n" +
          "Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"uploadedfile.txt\"\r\n" +
          "Content-Type: application/octet-stream\r\n" +
          "\r\n"
      val content = Array.ofDim[Byte](fileSize)
      val footer  =
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
  }
}
