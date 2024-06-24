/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.action

import java.nio.file.Files

import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api._
import play.api.data._
import play.api.data.format.Formats._
import play.api.data.Forms._
import play.api.libs.Files.TemporaryFile
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.PlaySpecification
import play.api.test.WithApplication
import play.api.test.WsTestClient

class FormActionSpec extends PlaySpecification with WsTestClient {
  import FormBinding.Implicits._

  case class User(
      name: String,
      email: String,
      age: Int
  )
  object User {
    def unapply(u: User): Option[(String, String, Port)] = Some(u.name, u.email, u.age)
  }

  val userForm = Form(
    mapping(
      "name"  -> of[String],
      "email" -> of[String],
      "age"   -> of[Int]
    )(User.apply)(User.unapply)
  )

  def application: Application = {
    val context = ApplicationLoader.Context.create(Environment.simple())
    new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
      import play.api.routing.sird.{ POST => SirdPost, _ }

      override lazy val actorSystem: ActorSystem            = ActorSystem("form-action-spec")
      implicit override lazy val materializer: Materializer = Materializer.matFromSystem(actorSystem)

      override def router: Router = Router.from {
        case SirdPost(p"/multipart") =>
          defaultActionBuilder(playBodyParsers.multipartFormData) { implicit request =>
            val user = userForm.bindFromRequest().get
            Ok(s"${user.name} - ${user.email}")
          }
        case SirdPost(p"/multipart/max-length") =>
          defaultActionBuilder(playBodyParsers.multipartFormData(1024)) { implicit request =>
            val user = userForm.bindFromRequest().get
            Ok(s"${user.name} - ${user.email}")
          }
        case SirdPost(p"/multipart/wrapped-max-length") =>
          defaultActionBuilder(playBodyParsers.maxLength(1024, playBodyParsers.multipartFormData)(this.materializer)) {
            implicit request =>
              val user = userForm.bindFromRequest().get
              Ok(s"${user.name} - ${user.email}")
          }
        case SirdPost(p"/tmpfileexists") =>
          // return true if the temporary file exists.
          // this is used to test if the temporary file is kept while processing the request.
          defaultActionBuilder(playBodyParsers.multipartFormData).async { implicit request =>
            request.body.file("file") match {
              case Some(file) =>
                for {
                  path <- Future(file.ref.path)
                  // make sure the temporary file is kept even if GC is called
                  _ <- Future(System.gc())
                  _ <- Future(Thread.sleep(100))
                } yield Ok(if (Files.exists(path)) "exists" else "not exists")
              case None =>
                Future.successful(BadRequest("No file"))
            }
          }
      }
    }.application
  }

  "Form Actions" should {
    "When POSTing" in {
      val multipartBody = MultipartFormData[TemporaryFile](
        dataParts = Map(
          "name"  -> Seq("Player"),
          "email" -> Seq("play@email.com"),
          "age"   -> Seq("10")
        ),
        files = Seq.empty,
        badParts = Seq.empty
      )

      "bind all parameters for multipart request" in new WithApplication(application) {
        override def running() = {
          val request = FakeRequest(POST, "/multipart").withMultipartFormDataBody(multipartBody)
          contentAsString(route(app, request).get) must beEqualTo("Player - play@email.com")
        }
      }

      "bind all parameters for multipart request with max length" in new WithApplication(application) {
        override def running() = {
          val request = FakeRequest(POST, "/multipart/max-length").withMultipartFormDataBody(multipartBody)
          contentAsString(route(app, request).get) must beEqualTo("Player - play@email.com")
        }
      }

      "bind all parameters for multipart request to temporary file" in new WithApplication(application) {
        override def running() = {
          val request = FakeRequest(POST, "/multipart/wrapped-max-length").withMultipartFormDataBody(multipartBody)
          contentAsString(route(app, request).get) must beEqualTo("Player - play@email.com")
        }
      }

      "keep TemporaryFiles of multipart request while processing" in new WithApplication(application) {
        override def running(): Unit = {
          val tempFileCreator = app.injector.instanceOf[TemporaryFileCreator]
          val tempFile        = tempFileCreator.create()
          Files.writeString(tempFile.path, "Hello")

          val multipartBody = MultipartFormData[TemporaryFile](
            dataParts = Map.empty,
            files = Seq(FilePart("file", "file.txt", Some("text/plain"), tempFile)),
            badParts = Seq.empty
          )
          val request = FakeRequest(POST, "/tmpfileexists").withMultipartFormDataBody(multipartBody)
          contentAsString(route(app, request).get) must beEqualTo("exists")
        }
      }
    }
  }
}
