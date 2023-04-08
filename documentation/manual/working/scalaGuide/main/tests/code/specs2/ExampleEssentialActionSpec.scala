/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests.specs2

import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._

// #scalatest-exampleessentialactionspec
class ExampleEssentialActionSpec extends PlaySpecification {
  "An essential action" should {
    "can parse a JSON body" in new WithApplication() with Injecting {
      override def running() = {
        val Action = inject[DefaultActionBuilder]
        val parse  = inject[PlayBodyParsers]

        val action: EssentialAction = Action(parse.json) { request =>
          val value = (request.body \ "field").as[String]
          Ok(value)
        }

        val request = FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "field": "value" }"""))

        val result = call(action, request)

        status(result) mustEqual OK
        contentAsString(result) mustEqual "value"
      }
    }
  }
}
// #scalatest-exampleessentialactionspec
