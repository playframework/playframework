/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests.specs2

// #scalatest-examplecontrollerspec
import play.api.mvc._
import play.api.test._

import scala.concurrent.Future

class ExampleControllerSpec extends PlaySpecification with Results {

  "Example Page#index" should {
    "should be valid" in {
      val controller = new ExampleController()
      val result: Future[Result] = controller.index().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must be equalTo "ok"
    }
  }
}
// #scalatest-examplecontrollerspec

// #scalatest-exampleformspec
class ExampleFormSpec extends PlaySpecification with Results {

  import play.api.data.Forms._
  import play.api.data._
  import play.api.i18n._
  import play.api.libs.json._

  val form = Form(
    mapping(
      "name" -> text,
      "age" -> number(min = 0)
    )(UserData.apply)(UserData.unapply)
  )

  case class UserData(name: String, age: Int)

  "Form" should {
    "be valid" in {
      val messagesApi = new DefaultMessagesApi(
        Map("en" ->
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

// #scalatest-examplecontroller
class ExampleController extends Controller {
  def index() = Action {
    Ok("ok")
  }
}
// #scalatest-examplecontroller
