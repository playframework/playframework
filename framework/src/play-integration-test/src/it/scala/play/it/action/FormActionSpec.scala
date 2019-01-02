/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.action

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.mvc.Results._
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication, WsTestClient }
import play.api.routing.Router

class FormActionSpec extends PlaySpecification with WsTestClient {

  case class User(
      name: String,
      email: String,
      age: Int
  )

  val userForm = Form(
    mapping(
      "name" -> of[String],
      "email" -> of[String],
      "age" -> of[Int]
    )(User.apply)(User.unapply)
  )

  def application: Application = {
    val context = ApplicationLoader.Context.create(Environment.simple())
    new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {

      import play.api.routing.sird.{ POST => SirdPost, _ }

      override lazy val actorSystem: ActorSystem = ActorSystem("form-action-spec")
      override implicit lazy val materializer: Materializer = ActorMaterializer()(this.actorSystem)

      override def router: Router = Router.from {
        case SirdPost(p"/multipart") => defaultActionBuilder(playBodyParsers.multipartFormData) { implicit request =>
          val user = userForm.bindFromRequest().get
          Ok(s"${user.name} - ${user.email}")
        }
        case SirdPost(p"/multipart/max-length") => defaultActionBuilder(playBodyParsers.multipartFormData(1024)) { implicit request =>
          val user = userForm.bindFromRequest().get
          Ok(s"${user.name} - ${user.email}")
        }
        case SirdPost(p"/multipart/wrapped-max-length") => defaultActionBuilder(playBodyParsers.maxLength(1024, playBodyParsers.multipartFormData)(this.materializer)) { implicit request =>
          val user = userForm.bindFromRequest().get
          Ok(s"${user.name} - ${user.email}")
        }
      }
    }.application
  }

  "Form Actions" should {

    "When POSTing" in {

      val multipartBody = MultipartFormData[TemporaryFile](
        dataParts = Map(
          "name" -> Seq("Player"),
          "email" -> Seq("play@email.com"),
          "age" -> Seq("10")
        ),
        files = Seq.empty,
        badParts = Seq.empty
      )

      "bind all parameters for multipart request" in new WithApplication(application) {
        val request = FakeRequest(POST, "/multipart").withMultipartFormDataBody(multipartBody)
        contentAsString(route(app, request).get) must beEqualTo("Player - play@email.com")
      }

      "bind all parameters for multipart request with max length" in new WithApplication(application) {
        val request = FakeRequest(POST, "/multipart/max-length").withMultipartFormDataBody(multipartBody)
        contentAsString(route(app, request).get) must beEqualTo("Player - play@email.com")
      }

      "bind all parameters for multipart request to temporary file" in new WithApplication(application) {
        val request = FakeRequest(POST, "/multipart/wrapped-max-length").withMultipartFormDataBody(multipartBody)
        contentAsString(route(app, request).get) must beEqualTo("Player - play@email.com")
      }
    }
  }
}
