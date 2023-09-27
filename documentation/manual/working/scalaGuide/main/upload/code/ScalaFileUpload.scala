/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.upload.fileupload {
  import java.io.File
  import java.nio.file.{ Files => JFiles }
  import java.nio.file.attribute.PosixFilePermission._
  import java.nio.file.attribute.PosixFilePermissions
  import java.nio.file.Path
  import java.nio.file.Paths

  import scala.concurrent.ExecutionContext

  import democontrollers._
  import org.apache.pekko.stream.scaladsl._
  import org.apache.pekko.stream.IOResult
  import org.apache.pekko.util.ByteString
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import play.api._
  import play.api.inject.guice.GuiceApplicationBuilder
  import play.api.libs.streams._
  import play.api.libs.Files
  import play.api.libs.Files.SingletonTemporaryFileCreator
  import play.api.mvc._
  import play.api.mvc.MultipartFormData.FilePart
  import play.api.test._
  import play.core.parsers.Multipart.FileInfo

  @RunWith(classOf[JUnitRunner])
  class ScalaFileUploadSpec extends AbstractController(Helpers.stubControllerComponents()) with PlaySpecification {
    import scala.concurrent.ExecutionContext.Implicits.global

    "A scala file upload" should {
      "upload file" in new WithApplication {
        override def running() = {
          val tmpFile = JFiles.createTempFile(null, null)
          writeFile(tmpFile, "hello")

          new File("/tmp/picture").mkdirs()
          val uploaded = new File("/tmp/picture/formuploaded")
          uploaded.delete()

          val parse  = app.injector.instanceOf[PlayBodyParsers]
          val Action = app.injector.instanceOf[DefaultActionBuilder]

          // #upload-file-action
          def upload: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { request =>
            request.body
              .file("picture")
              .map { picture =>
                // only get the last part of the filename
                // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
                val filename    = Paths.get(picture.filename).getFileName
                val fileSize    = picture.fileSize
                val contentType = picture.contentType

                picture.ref.copyTo(Paths.get(s"/tmp/picture/$filename"), replace = true)
                Ok("File uploaded")
              }
              .getOrElse {
                Redirect(routes.HomeController.index()).flashing("error" -> "Missing file")
              }
          }

          // #upload-file-action
          val temporaryFileCreator = SingletonTemporaryFileCreator
          val tf                   = temporaryFileCreator.create(tmpFile)
          val request = FakeRequest().withBody(
            MultipartFormData(Map.empty, Seq(FilePart("picture", "formuploaded", None, tf, JFiles.size(tf.path))), Nil)
          )
          testAction(upload, request)

          uploaded.delete()
          success
        }
      }

      "upload file directly" in new WithApplication {
        override def running() = {
          val tmpFile = Paths.get("/tmp/picture/tmpuploaded")
          writeFile(tmpFile, "hello")

          new File("/tmp/picture").mkdirs()
          val uploaded = new File("/tmp/picture/uploaded")
          uploaded.delete()

          val temporaryFileCreator = SingletonTemporaryFileCreator
          val tf                   = temporaryFileCreator.create(tmpFile)

          val request = FakeRequest().withBody(tf)

          val controllerComponents = app.injector.instanceOf[ControllerComponents]
          testAction(new democontrollers.HomeController(controllerComponents).upload, request)

          uploaded.delete()
          success
        }
      }
    }

    private def testAction[A](action: Action[A], request: => Request[A] = FakeRequest(), expectedResponse: Int = OK)(
        implicit app: Application
    ) = {
      val result = action(request)
      status(result) must_== expectedResponse
    }

    def writeFile(file: File, content: String): Path = {
      writeFile(file.toPath, content)
    }

    def writeFile(path: Path, content: String): Path = {
      JFiles.write(path, content.getBytes)
    }
  }

  // Not using `controllers` as package name because it produces resolution collisions
  // in callsites that also import `play.api._` in Scala 2.13
  package democontrollers {

    import play.api.libs.Files

    class HomeController(controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
        extends AbstractController(controllerComponents) {
      // #upload-file-directly-action
      def upload: Action[Files.TemporaryFile] = Action(parse.temporaryFile) { request =>
        request.body.moveTo(Paths.get("/tmp/picture/uploaded"), replace = true)
        Ok("File uploaded")
      }
      // #upload-file-directly-action

      def index: Action[AnyContent] = Action { request => Ok("Upload failed") }

      // #upload-file-customparser
      type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

      def handleFilePartAsFile: FilePartHandler[File] = {
        case FileInfo(partName, filename, contentType, dispositionType) =>
          val perms       = java.util.EnumSet.of(OWNER_READ, OWNER_WRITE)
          val attr        = PosixFilePermissions.asFileAttribute(perms)
          val path        = JFiles.createTempFile("multipartBody", "tempFile", attr)
          val file        = path.toFile
          val fileSink    = FileIO.toPath(path)
          val accumulator = Accumulator(fileSink)
          accumulator.map {
            case IOResult(count, status) =>
              FilePart(partName, filename, contentType, file, count, dispositionType)
          }(ec)
      }

      def uploadCustom: Action[MultipartFormData[File]] = Action(parse.multipartFormData(handleFilePartAsFile)) {
        request =>
          val fileOption = request.body.file("name").map {
            case FilePart(key, filename, contentType, file, fileSize, dispositionType, _) =>
              file.toPath
          }

          Ok(s"File uploaded: $fileOption")
      }
      // #upload-file-customparser
    }
  }
}
