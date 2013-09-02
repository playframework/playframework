package scalaguide.ws.scalaws {

import play.api.libs.ws._
import play.api.libs.iteratee._
import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

import java.io._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.libs.Files.TemporaryFile

@RunWith(classOf[JUnitRunner])
class ScalaWSSpec extends PlaySpecification with Results {

  "WS get" should {

    // Create a route that will lead us to a file we'll download in chunks.
    val fakeApplication: FakeApplication = FakeApplication(withRoutes = {
      case ("GET", "/") =>
        Action {
          // Create an input stream of lots of null bytes.
          val inputStream: InputStream = new InputStream() {
            private var count = 0
            private val numBytes = 4096

            def read(): Int = {
              count = count + 1
              if (count > numBytes) -1 else 0
            }
          }
          implicit val context = scala.concurrent.ExecutionContext.Implicits.global

          Ok.chunked(Enumerator.fromStream(inputStream))
        }
      case (_, _) =>
        Action {
          BadRequest("no binding found")
        }
    })

    "work with an iteratee" in new WithServer(fakeApplication, 3333) {
      implicit val context = scala.concurrent.ExecutionContext.Implicits.global

      val file = File.createTempFile("scalaws", "")
      try {
        // #scalaws-fileupload
        val timeout = 3000
        val outputStream: OutputStream = new BufferedOutputStream(new FileOutputStream(file))
        val futureResponse = WS.url("http://localhost:3333/").withTimeout(timeout).get({
          headers =>
            Iteratee.fold[Array[Byte], OutputStream](outputStream) {
              (stream, bytes) =>
                stream.write(bytes)
                stream
            }
        }).map {
          iteratee =>
            outputStream.flush()
            outputStream.close()
            iteratee
        }
        // #scalaws-fileupload
        await(futureResponse, 4000)

        file.exists() should beTrue
        file.length() should beEqualTo(4096)
      } finally {
        file.delete()
      }
    }
  }

}

}

