/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.http.scalaactions {
  import akka.util.ByteString
  import play.api.mvc._
  import play.api.test._
  import play.api.test.Helpers._
  import org.specs2.mutable.Specification
  import org.specs2.mutable.SpecificationLike
  import play.api.libs.json._

  import scala.concurrent.Future
  import org.specs2.execute.AsResult

  class ScalaActionsSpec extends AbstractController(Helpers.stubControllerComponents()) with SpecificationLike {
    "A scala action" should {
      "allow writing a simple echo action" in {
        //#echo-action
        def echo = Action { request =>
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

      "pass the request implicitly to the action with more methods" in {
        //#implicit-request-action-with-more-methods
        def action = Action { implicit request =>
          anotherMethod("Some para value")
          Ok("Got request [" + request + "]")
        }

        def anotherMethod(p: String)(implicit request: Request[_]) = {
          // do something that needs access to the request
        }
        //#implicit-request-action-with-more-methods
        testAction(action)
      }

      "allow specifying a parser" in {
        testAction(
          action = //#json-parser-action
            Action(parse.json) { implicit request =>
              Ok("Got request [" + request + "]")
            }
          //#json-parser-action
          ,
          request = FakeRequest().withBody(Json.obj()).withHeaders(CONTENT_TYPE -> "application/json")
        )
      }

      "work for a full controller class" in {
        testAction(new full.Application(Helpers.stubControllerComponents()).index)
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
        import play.api.http.HttpEntity

        def index = Action {
          Result(
            header = ResponseHeader(200, Map.empty),
            body = HttpEntity.Strict(ByteString("Hello world!"), Some("text/plain"))
          )
        }
        //#simple-result-action
        assertAction(index) { result =>
          contentAsString(result) must_== "Hello world!"
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
        val ok           = Ok("Hello world!")
        val notFound     = NotFound
        val pageNotFound = NotFound(<h1>Page not found</h1>)
        val badRequest   = BadRequest(views.html.form(formWithErrors))
        val oops         = InternalServerError("Oops")
        val anyStatus    = Status(488)("Strange response type")
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
          (header(LOCATION, result) must be).some("/user/home")
        }
      }

      "support other redirects" in {
        //#moved-permanently-action
        def index = Action {
          Redirect("/user/home", MOVED_PERMANENTLY)
        }
        //#moved-permanently-action
        assertAction(index, expectedResponse = MOVED_PERMANENTLY) { result =>
          (header(LOCATION, result) must be).some("/user/home")
        }
      }

      "support todo actions" in {
        //#todo-action
        def index(name: String) = TODO
        //#todo-action
        testAction(index("foo"), expectedResponse = NOT_IMPLEMENTED)
      }
    }

    def testAction[A](action: Action[A], expectedResponse: Int = OK, request: Request[A] = FakeRequest()) = {
      assertAction(action, expectedResponse, request) { result =>
        success
      }
    }

    def assertAction[A, T: AsResult](
        action: Action[A],
        expectedResponse: Int = OK,
        request: Request[A] = FakeRequest()
    )(assertions: Future[Result] => T) = {
      running() { _ =>
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

package scalaguide.gettingstarted.scalacontrollers.full {

  //#gettingStartedController
  //###insert: package controllers

  import javax.inject.Inject

  // Helpful implicits for your controler come from here, like AbstractControler and ControllerComponents.
  import play.api.mvc._

  // Controllers are constructed through dependency injection using Guice.
  class GettingStartedController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

    // HTTP 200
    def index = Action {
      // When we extend AbstractController we get some helpful Wrapper
      Ok("It works!")
    }

    // HTTP 200
    def redirectTarget = Action {
      // When we extend AbstractController we get some helpful Wrapper
      Ok("You've been redirected!")
    }

    // HTTP 302
    def redirect = Action {
      Redirect("/redirect-target")
    }

    // HTTP 301
    def permanentRedirect(location: String) = Action {
      // Be careful with 301 redirects, they are very very hard to remove as many different layers of network caching
      // will not forget the location. 9 times out of 10 you're just as well off with a 302 or 307.
      PermanentRedirect(location, MOVED_PERMANENTLY)
    }

    // HTTP 301
    def permanentRedirectUsingHelper = Action {
      PermanentRedirect("/redirect-target")
    }

    // HTTP 404
    def notFound = Action {
      NotFound("Your page was not found.")
    }

    def valueFromUri(number: Int) = Action {
      Ok(s"Your number was $number")
    }
  }
  //#gettingStartedController
}

package scalaguide.gettingstarted.scalacontrollers.full {

  //#gettingStartedControllerTwirl
  //###insert: package controllers

  import javax.inject.Inject

  // Helpful implicits for your controler come from here, like AbstractControler and ControllerComponents.
  import play.api.mvc._

  // Controllers are constructed through dependency injection using Guice.
  class GettingStartedController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

    def index = Action {
      Ok(views.html.ourTwirlTemplate())
    }

  }
  //#gettingStartedControllerTwirl

}
