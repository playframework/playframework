package scalaguide.upload.fileupload {

  import play.api.mvc._
  import play.api.test._
  import play.api.test.Helpers._
  import org.specs2.mutable.Specification
  import play.api.libs.json._
  import play.api.libs.iteratee.Enumerator
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import scala.concurrent.Future
  import java.io.File

  import controllers._

  @RunWith(classOf[JUnitRunner])
  class ScalaFileUploadSpec extends Specification with Controller {

    "A scala file upload" should {

      "upload file" in {
        new File("/tmp/picture").mkdirs
        //#upload-file-action
        def upload = Action(parse.multipartFormData) { request =>
          request.body.file("picture").map { picture =>
            import java.io.File
            val filename = picture.filename
            val contentType = picture.contentType
            picture.ref.moveTo(new File(s"/tmp/picture/$filename"))
            Ok("File uploaded")
          }.getOrElse {
            Redirect(routes.Application.index).flashing(
              "error" -> "Missing file")
          }
        }
        //#upload-file-action

        val request = FakeRequest().withTextBody("hello").withHeaders(CONTENT_TYPE -> "text/plain")
        testAction(upload, request, BAD_REQUEST)
      }

      "upload file directly" in {
        
        new File("/tmp/picture").mkdirs
        new File("/tmp/picture/uploaded").delete
        def upload = controllers.Application.upload
        
        val request = FakeRequest().withTextBody("hello").withHeaders(CONTENT_TYPE -> "text/plain")
        testAction(upload, request)

      }

      def testAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) {
        assertAction(action, request, expectedResponse) { result => }
      }

      def assertAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK)(assertions: Future[SimpleResult] => Unit) {
        running(FakeApplication(additionalConfiguration = Map("application.secret" -> "pass"))) {
          val result = action(request).run
          status(result) must_== expectedResponse
          assertions(result)
        }
      }
    }
  }
  package controllers {
    object Application extends Controller {

      //#upload-file-directly-action
        def upload = Action(parse.temporaryFile) { request =>
          request.body.moveTo(new File("/tmp/picture/uploaded"))
          Ok("File uploaded")
        }
        //#upload-file-directly-action

      def index = Action { request =>
        Ok("Upload failed")
      }

    }
  }
}
  