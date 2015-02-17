/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.http.scalaactions {

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Specification
import play.api.libs.json._
import play.api.libs.iteratee.Enumerator
import scala.concurrent.Future
import org.specs2.execute.AsResult

object ScalaActionsSpec extends Specification with Controller {

  "A scala action" should {
    "allow writing a simple echo action" in {
      //#echo-action
      val echo = Action { request =>
        Ok("Got request [" + request + "]")
      }
      //#echo-action
      testAction(echo)
    }

    "support zero arg actions" in {
      testAction(
        //#zero-arg-action
        Action {
          Ok("Hello world")
        }
        //#zero-arg-action
      )
    }

    "pass the request to the action" in {
      testAction(
        //#request-action
        Action { request =>
          Ok("Got request [" + request + "]")
        }
        //#request-action
      )
    }

    "pass the request implicitly to the action" in {
      testAction(
        //#implicit-request-action
        Action { implicit request =>
          Ok("Got request [" + request + "]")
        }
        //#implicit-request-action
      )
    }

    "allow specifying a parser" in {
      testAction(action =
        //#json-parser-action
        Action(parse.json) { implicit request =>
          Ok("Got request [" + request + "]")
        }
        //#json-parser-action
        , request = FakeRequest().withBody(Json.obj()).withHeaders(CONTENT_TYPE -> "application/json")
      )
    }

    "work for a full controller class" in {
      testAction(new full.Application().index)
    }

    "support an action with parameters" in {
      //#parameter-action
      def hello(name: String) = Action {
        Ok("Hello " + name)
      }
      //#parameter-action

      assertAction(hello("world")) { result =>
        contentAsString(result) must_== "Hello world"
      }
    }

    "support returning a simple result" in {
      //#simple-result-action
      def index = Action {
        Result(
          header = ResponseHeader(200, Map(CONTENT_TYPE -> "text/plain")),
          body = Enumerator("Hello world!".getBytes())
        )
      }
      //#simple-result-action
      assertAction(index) { result =>
        contentAsString(result) must_== "Hello world!"
        header(CONTENT_TYPE, result) must be some "text/plain"
      }
    }

    "support ok helper" in {
      //#ok-result-action
      def index = Action {
        Ok("Hello world!")
      }
      //#ok-result-action
      testAction(index)
    }

    "support other result types" in {
      val formWithErrors = ""

      //#other-results
      val ok = Ok("Hello world!")
      val notFound = NotFound
      val pageNotFound = NotFound(<h1>Page not found</h1>)
      val badRequest = BadRequest(views.html.form(formWithErrors))
      val oops = InternalServerError("Oops")
      val anyStatus = Status(488)("Strange response type")
      //#other-results

      anyStatus.header.status must_== 488
    }

    "support redirects" in {
      //#redirect-action
      def index = Action {
        Redirect("/user/home")
      }
      //#redirect-action
      assertAction(index, expectedResponse = SEE_OTHER) { result =>
        header(LOCATION, result) must be some "/user/home"
      }
    }

    "support other redirects" in {
      //#moved-permanently-action
      def index = Action {
        Redirect("/user/home", MOVED_PERMANENTLY)
      }
      //#moved-permanently-action
      assertAction(index, expectedResponse = MOVED_PERMANENTLY) { result =>
        header(LOCATION, result) must be some "/user/home"
      }
    }

    "support todo actions" in {
      //#todo-action
      def index(name:String) = TODO
      //#todo-action
      testAction(index("foo"), expectedResponse = NOT_IMPLEMENTED)
    }

  }

  def testAction[A](action: Action[A], expectedResponse: Int = OK, request: Request[A] = FakeRequest()) = {
    assertAction(action, expectedResponse, request) { result => success }
  }

  def assertAction[A, T: AsResult](action: Action[A], expectedResponse: Int = OK, request: Request[A] = FakeRequest())(assertions: Future[Result] => T) = {
    running(FakeApplication()) {
      val result = action(request)
      status(result) must_== expectedResponse
      assertions(result)
    }
  }
}

// Faking a form view
package views.html {
  object form {
    def apply(obj: AnyRef): String = ""
  }
}
}

package scalaguide.http.scalaactions.full {
//#full-controller
//###insert: package controllers

import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Ok("It works!")
  }

}
//#full-controller
}
