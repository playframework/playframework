/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package specs2

import scala.concurrent.Future

import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.ControllerHelpers
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.PlaySpecification

// #scalatest-examplemessagesspec
class ExampleMessagesSpec extends PlaySpecification with ControllerHelpers {
  import play.api.data.Form
  import play.api.data.FormBinding.Implicits._
  import play.api.data.Forms._
  import play.api.i18n._
  import play.api.libs.json.Json

  case class UserData(name: String, age: Int)
  object UserData {
    def unapply(u: UserData): Option[(String, Int)] = Some(u.name, u.age)
  }

  "Messages test" should {
    "test messages validation in forms" in {
      // Define a custom message against the number validation constraint
      val messagesApi = new DefaultMessagesApi(
        Map("en" -> Map("error.min" -> "CUSTOM MESSAGE"))
      )

      // Called when form validation fails
      def errorFunc(badForm: Form[UserData])(implicit request: RequestHeader) = {
        implicit val messages: Messages = messagesApi.preferred(request)
        BadRequest(badForm.errorsAsJson)
      }

      // Called when form validation succeeds
      def successFunc(userData: UserData) = Redirect("/")

      // Define an age with 0 as the minimum
      val form = Form(
        mapping("name" -> text, "age" -> number(min = 0))(UserData.apply)(UserData.unapply)
      )

      // Submit a request with age = -1
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = {
        play.api.test
          .FakeRequest("POST", "/")
          .withFormUrlEncodedBody("name" -> "Play", "age" -> "-1")
      }

      // Verify that the "error.min" is the custom message
      val result = Future.successful(form.bindFromRequest().fold(errorFunc, successFunc))
      Json.parse(contentAsString(result)) must beEqualTo(Json.obj("age" -> Json.arr("CUSTOM MESSAGE")))
    }
  }
}
// #scalatest-examplemessagesspec
