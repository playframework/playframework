/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests.specs2

// #scalatest-examplecontrollerspec
import javax.inject.Inject

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

// #scalatest-exampleform
class ExampleFormSpec extends PlaySpecification with Results {

  import play.api.data._
  import play.api.data.Forms._

  case class UserData(name: String, age: Int)

  val form = Form(
    mapping(
      "name" -> text,
      "age" -> number(min = 0)
    )(UserData.apply)(UserData.unapply)
  )

  "Form" should {
    "be valid" in {
      import play.api.i18n.DefaultMessagesApi
      implicit val messagesApi = new DefaultMessagesApi(Map("en" -> Map("constraint.min" -> "minimum!")))

      def errorFunc(badForm: Form[UserData]) = {
        BadRequest(badForm.errorsAsJson)
      }
      def successFunc(userData: UserData) = {
        Redirect("/").flashing("success" -> "success form!")
      }

      implicit val request = FakeRequest("POST", "/").withFormUrlEncodedBody("name" -> "Play", "age" -> "-1")
      val result: Result = form.bindFromRequest().fold(errorFunc, successFunc)
      result.header.status must be 307
    }
  }
}
// #scalatest-exampleform

// #scalatest-examplecontroller
class ExampleController extends Controller {
  def index() = Action {
    Ok("ok")
  }
}
// #scalatest-examplecontroller
