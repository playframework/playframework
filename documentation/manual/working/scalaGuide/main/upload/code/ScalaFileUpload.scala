/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.upload.fileupload {

  import play.api.mvc._
  import play.api.test._
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import java.io.{FileWriter, FileOutputStream, File}

  import controllers._
  import play.api.libs.Files.TemporaryFile
  import play.api.mvc.MultipartFormData.FilePart

  @RunWith(classOf[JUnitRunner])
  class ScalaFileUploadSpec extends PlaySpecification with Controller {

    "A scala file upload" should {

      "upload file" in {
        val tmpFile = new File("/tmp/picture/tmpformuploaded")
        writeFile(tmpFile, "hello")

        new File("/tmp/picture").mkdirs()
        val uploaded = new File("/tmp/picture/formuploaded")
        uploaded.delete()

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

        val request = FakeRequest().withBody(
          MultipartFormData(Map.empty, Seq(FilePart("picture", "formuploaded", None, TemporaryFile(tmpFile))), Nil, Nil)
        )
        testAction(upload, request)

        uploaded.delete()
        success
      }

      "upload file directly" in {
        val tmpFile = new File("/tmp/picture/tmpuploaded")
        writeFile(tmpFile, "hello")

        new File("/tmp/picture").mkdirs()
        val uploaded = new File("/tmp/picture/uploaded")
        uploaded.delete()

        val request = FakeRequest().withBody(TemporaryFile(tmpFile))
        testAction(new controllers.Application().upload, request)

        uploaded.delete()
        success
      }
    }

    def testAction[A](action: Action[A], request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) = {
      running(FakeApplication()) {

        val result = action(request)

        status(result) must_== expectedResponse
      }
    }

    def writeFile(file: File, content: String) = {
      file.getParentFile.mkdirs()
      val out = new FileWriter(file)
      try {
        out.write(content)
      } finally {
        out.close()
      }
    }

  }
  package controllers {
    class Application extends Controller {

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
  
