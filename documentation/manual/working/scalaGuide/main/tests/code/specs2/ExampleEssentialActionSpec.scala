/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.specs2

import play.api.mvc._
import play.api.test._
import play.api.mvc.Results._
import play.api.libs.json.Json

// #scalatest-exampleessentialactionspec
object ExampleEssentialActionSpec extends PlaySpecification {

  "An essential action" should {
    "can parse a JSON body" in {
      val action: EssentialAction = Action { request =>
        val value = (request.body.asJson.get \ "field").as[String]
        Ok(value)
      }

      val request = FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "field": "value" }"""))

      val result = call(action, request)

      status(result) mustEqual OK
      contentAsString(result) mustEqual "value"
    }
  }
}
// #scalatest-exampleessentialactionspec
