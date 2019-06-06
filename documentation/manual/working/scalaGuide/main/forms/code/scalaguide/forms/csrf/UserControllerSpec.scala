/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.forms.csrf

import javax.inject.Inject

import org.specs2.mutable.Specification
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents

class UserController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def userGet = Action {
    Ok("success").as("text/html")
  }
}

// #testing-csrf
import play.api.test.Helpers._
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.WithApplication

class UserControllerSpec extends Specification {
  "UserController GET" should {

    "render the index page from the application" in new WithApplication() {
      val controller = app.injector.instanceOf[UserController]
      val request    = FakeRequest().withCSRFToken
      val result     = controller.userGet().apply(request)

      status(result) must beEqualTo(OK)
      contentType(result) must beSome("text/html")
    }
  }
}
// #testing-csrf
