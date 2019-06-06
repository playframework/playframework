/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests.specs2

// #scalatest-examplecontrollerspec
import javax.inject.Inject

import play.api.i18n.Messages
import play.api.mvc._
import play.api.test._

import scala.concurrent.Future

class ExampleControllerSpec extends PlaySpecification with Results {

  "Example Page#index" should {
    "be valid" in {
      val controller             = new ExampleController(Helpers.stubControllerComponents())
      val result: Future[Result] = controller.index().apply(FakeRequest())
      val bodyText: String       = contentAsString(result)
      (bodyText must be).equalTo("ok")
    }
  }

}
// #scalatest-examplecontrollerspec

// #scalatest-exampleformspec
object FormData {
  import play.api.data.Forms._
  import play.api.data._
  import play.api.i18n._
  import play.api.libs.json._

  val form = Form(
    mapping(
      "name" -> text,
      "age"  -> number(min = 0)
    )(UserData.apply)(UserData.unapply)
  )

  case class UserData(name: String, age: Int)
}

class ExampleFormSpec extends PlaySpecification with Results {

  import play.api.data._
  import play.api.i18n._
  import play.api.libs.json._
  import FormData._

  "Form" should {
    "be valid" in {
      val messagesApi = new DefaultMessagesApi(
        Map(
          "en" ->
            Map("error.min" -> "minimum!")
        )
      )
      implicit val request = {
        FakeRequest("POST", "/")
          .withFormUrlEncodedBody("name" -> "Play", "age" -> "-1")
      }
      implicit val messages = messagesApi.preferred(request)

      def errorFunc(badForm: Form[UserData]) = {
        BadRequest(badForm.errorsAsJson)
      }

      def successFunc(userData: UserData) = {
        Redirect("/").flashing("success" -> "success form!")
      }

      val result = Future.successful(form.bindFromRequest().fold(errorFunc, successFunc))
      Json.parse(contentAsString(result)) must beEqualTo(Json.obj("age" -> Json.arr("minimum!")))
    }
  }
}
// #scalatest-exampleformspec

// #scalatest-exampletemplatespec
class ExampleTemplateSpec extends PlaySpecification {
  import play.api.data._
  import FormData._

  "Example Template with Form" should {
    "be valid" in {
      val form: Form[UserData]        = FormData.form
      implicit val messages: Messages = Helpers.stubMessages()
      contentAsString(views.html.formTemplate(form)) must contain("ok")
    }
  }
}
// #scalatest-exampletemplatespec

// #scalatest-examplecsrftemplatespec
class ExampleTemplateWithCSRFSpec extends PlaySpecification {
  import play.api.data._
  import FormData._

  "Example Template with Form" should {
    "be valid" in {
      val form: Form[UserData]                                 = FormData.form
      implicit val messageRequestHeader: MessagesRequestHeader = Helpers.stubMessagesRequest()
      contentAsString(views.html.formTemplateWithCSRF(form)) must contain("ok")
    }
  }
}
// #scalatest-examplecsrftemplatespec

// #scalatest-examplecontroller
class ExampleController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def index() = Action {
    Ok("ok")
  }
}
// #scalatest-examplecontroller
