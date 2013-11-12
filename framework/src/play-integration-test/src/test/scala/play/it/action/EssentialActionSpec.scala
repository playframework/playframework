/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.action

import play.api.mvc.Results._
import play.api.test.{PlaySpecification, FakeRequest}
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{SimpleResult, Action, EssentialAction}
import scala.concurrent.Future

object EssentialActionSpec extends PlaySpecification {

  "an EssentialAction" should {
    "be tested without any started FakeApplication" in {

      val action: EssentialAction = Action { request =>
        val value = (request.body.asJson.get \ "field").as[String]
        Ok(value)
      }

      val request = FakeRequest(POST, "/").withHeaders(CONTENT_TYPE -> "application/json")

      val requestBody = Enumerator("""{ "field": "value" }""".getBytes) andThen Enumerator.eof
      val result: Future[SimpleResult] = requestBody |>>> action(request)
      status(result) mustEqual  OK
      contentAsString(result) mustEqual "value"
    }
  }

}
