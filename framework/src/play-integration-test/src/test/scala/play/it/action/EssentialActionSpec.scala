/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.action

import play.api.mvc.{Action, EssentialAction}
import play.api.mvc.Results._
import play.api.test.{FakeApplication, PlaySpecification, FakeRequest}
import play.api.libs.iteratee.Enumerator
import scala.concurrent.Promise

object EssentialActionSpec extends PlaySpecification {

  "an EssentialAction" should {
    "be tested without any started FakeApplication" in {

      val action: EssentialAction = Action { request =>
        val value = (request.body.asJson.get \ "field").as[String]
        Ok(value)
      }

      val request = FakeRequest(POST, "/").withHeaders(CONTENT_TYPE -> "application/json")

      val requestBody = Enumerator("""{ "field": "value" }""".getBytes) andThen Enumerator.eof
      val result = requestBody |>>> action(request)
      status(result) mustEqual  OK
      contentAsString(result) mustEqual "value"
    }


    "use the classloader of the running application" in {

      val actionClassLoader = Promise[ClassLoader]()
      val action: EssentialAction = Action {
        actionClassLoader.success(Thread.currentThread.getContextClassLoader)
        Ok("")
      }

      // start fake application with its own classloader
      val applicationClassLoader = new ClassLoader(){}
      val fakeApplication = FakeApplication(classloader = applicationClassLoader)

      running(fakeApplication) {
        // run the test with the classloader of the current thread
        Thread.currentThread.getContextClassLoader must not be applicationClassLoader
        action.apply(FakeRequest()).run
        await(actionClassLoader.future) must be equalTo applicationClassLoader
      }
    }
  }

}
