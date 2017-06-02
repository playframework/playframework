/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.action

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.BodyParsers._
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }

class FormActionSpec extends PlaySpecification {

  val userForm = Form(
    mapping(
      "name" -> of[String],
      "email" -> of[String],
      "age" -> of[Int]
    )(User.apply)(User.unapply)
  )

  def application: Application = {

    implicit val actorSystem = ActorSystem("form-action-spec")
    implicit val materializer = ActorMaterializer()

    GuiceApplicationBuilder()
      .overrides(
        play.api.inject.bind[ActorSystem].toInstance(actorSystem),
        play.api.inject.bind[Materializer].toInstance(materializer)
      )
      .routes {
        case (POST, "/multipart") => Action(parse.multipartFormData) { implicit request =>
          val user = userForm.bindFromRequest().get
          Ok(s"${user.name} - ${user.email}")
        }
        case (POST, "/multipart/max-length") => Action(parse.multipartFormData(1024)) { implicit request =>
          val user = userForm.bindFromRequest().get
          Ok(s"${user.name} - ${user.email}")
        }
        case (POST, "/multipart/wrapped-max-length") => Action(parse.maxLength(1024, parse.multipartFormData)) { implicit request =>
          val user = userForm.bindFromRequest().get
          Ok(s"${user.name} - ${user.email}")
        }
      }.build()
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

case class User(
  name: String,
  email: String,
  age: Int
)
