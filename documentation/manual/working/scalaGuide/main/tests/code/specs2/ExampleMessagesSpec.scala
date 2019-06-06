/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package specs2

import play.api.mvc.ControllerHelpers
import play.api.mvc.RequestHeader
import play.api.test.PlaySpecification

import scala.concurrent.Future

// #scalatest-examplemessagesspec
class ExampleMessagesSpec extends PlaySpecification with ControllerHelpers {
  import play.api.libs.json.Json
  import play.api.data.Forms._
  import play.api.data.Form
  import play.api.i18n._

  case class UserData(name: String, age: Int)

  "Messages test" should {
    "test messages validation in forms" in {
      // Define a custom message against the number validation constraint
      val messagesApi = new DefaultMessagesApi(
        Map("en" -> Map("error.min" -> "CUSTOM MESSAGE"))
      )

      // Called when form validation fails
      def errorFunc(badForm: Form[UserData])(implicit request: RequestHeader) = {
        implicit val messages = messagesApi.preferred(request)
        BadRequest(badForm.errorsAsJson)
      }

      // Called when form validation succeeds
      def successFunc(userData: UserData) = Redirect("/")

      // Define an age with 0 as the minimum
      val form = Form(
        mapping("name" -> text, "age" -> number(min = 0))(UserData.apply)(UserData.unapply)
      )

      // Submit a request with age = -1
      implicit val request = {
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
