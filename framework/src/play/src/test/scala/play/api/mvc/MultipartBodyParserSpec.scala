package play.api.mvc

import java.io.IOException
import java.nio.file.{ Files, Paths }

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ FileIO, Source }
import akka.util.ByteString
import org.specs2.mutable.Specification
import play.api.http.{ HttpEntity, HttpErrorHandler }
import play.core.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.util.Random

class MultipartBodyParserSpec extends Specification {

  "Multipar body parser" should {
    implicit val system = ActorSystem()
    implicit val executionContext = system.dispatcher
    implicit val materializer = ActorMaterializer()

    class CustomHttpErrorHandler extends HttpErrorHandler {
      def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = ???
      def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
        val responseHeader = new ResponseHeader(status = 500, reasonPhrase = Some(exception.getMessage))
        Future.successful(Result(header = responseHeader, body = HttpEntity.NoEntity))
      }
    }

    val playBodyParsers = PlayBodyParsers(
      tfc = new InMemoryTemporaryFileCreator(10),
      eh = new CustomHttpErrorHandler)

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
          Nil)

      val bodySize = header.length + fileSize + footer.length

      val request = FakeRequest(
        method = "POST",
        uri = "/x",
        headers = FakeHeaders(Seq(
          "Content-Type" -> s"multipart/form-data; boundary=$boundary",
          "Content-Length" -> bodySize.toString)),
        body = body)

      val response = playBodyParsers.multipartFormData.apply(request).run(body)
      Await.result(response, Duration.Inf) must beLeft.like {
        case result =>
          result.header.status must_=== 500
          result.header.reasonPhrase must beSome("Out of disk space")
      }
    }
  }
}
