/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.scalatest

// #scalatest-examplecontrollerspec
import scala.concurrent.Future

import org.scalatest._
import org.scalatestplus.play._

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

class ExampleControllerSpec extends PlaySpec with Results {

  //#scalatest-examplepost
  trait WithControllerAndRequest {
    val testController = new Controller with ApiController

    def fakeRequest(method: String = "GET", route: String = "/") = FakeRequest(method, route)
      .withHeaders(
        ("Date", "2014-10-05T22:00:00"),
        ("Authorization", "username=bob;hash=foobar==")
    )
  }

  "REST API" should {
    "create a new user" in new WithControllerAndRequest {
      val request = fakeRequest("POST", "/user").withJsonBody(Json.parse(
        s"""{"first_name": "Alice",
          |  "last_name": "Doe",
          |  "credentials": {
          |    "username": "alice",
          |    "password": "secret"
          |  }
          |}""".stripMargin))
      val apiResult = call(testController.createUser, request)
      status(apiResult) mustEqual CREATED
      val jsonResult = contentAsJson(apiResult)
      ObjectId.isValid((jsonResult \ "id").as[String]) mustBe true

      // now get the real thing from the DB and check it was created with the correct values:
      val newbie = Dao().findByUsername("alice").get
      newbie.id.get.toString mustEqual (jsonResult \ "id").as[String]
      newbie.firstName mustEqual "Alice"
    }
  }
  //#scalatest-examplepost
}
