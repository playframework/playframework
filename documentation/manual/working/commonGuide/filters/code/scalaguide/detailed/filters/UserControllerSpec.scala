/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.detailed.filters

// #test-with-withCSRFToken
import org.specs2.mutable.Specification
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers._
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
// #test-with-withCSRFToken

// #test-disabling-filters
class UserControllerWithoutFiltersSpec extends Specification {
  "UserControllerWithoutFiltersSpec GET" should {

    "render the index page from the application" in new WithApplication(
      GuiceApplicationBuilder().configure("play.http.filters" -> "play.api.http.NoHttpFilters").build()
    ) {

      val controller = app.injector.instanceOf[UserController]
      val request    = FakeRequest().withCSRFToken
      val result     = controller.userGet().apply(request)

      status(result) must beEqualTo(OK)
      contentType(result) must beSome("text/html")
    }
  }
}
// #test-disabling-filters

import javax.inject.Inject
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents

class UserController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def userGet = Action {
    Ok("success").as(HTML)
  }
}
