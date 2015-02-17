/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.http.scalacontentnegotiation {

  import play.api.mvc._
  import play.api.test._
  import play.api.test.Helpers._
  import org.specs2.mutable.Specification
  import play.api.libs.json._
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import scala.concurrent.Future
  import org.specs2.execute.AsResult

  @RunWith(classOf[JUnitRunner])
  class ScalaContentNegotiation extends Specification with Controller {

    "A Scala Content Negotiation" should {
      "negotiate accept type" in {
        //#negotiate_accept_type
        val list = Action { implicit request =>
          val items = Item.findAll
          render {
            case Accepts.Html() => Ok(views.html.list(items))
            case Accepts.Json() => Ok(Json.toJson(items))
          }
        }
        //#negotiate_accept_type

        val requestHtml = FakeRequest().withHeaders(ACCEPT -> "text/html")
        assertAction(list, OK, requestHtml)(r => contentAsString(r) === "<html>1,2,3</html>")

        val requestJson = FakeRequest().withHeaders(ACCEPT -> "application/json")
        assertAction(list, OK, requestJson)(r => contentAsString(r) === "[1,2,3]")
      }

      "negotiate accept type" in {
        
        val list = Action { implicit request =>

          def ??? = Ok("ok")
          //#extract_custom_accept_type
          val AcceptsMp3 = Accepting("audio/mp3")
          render {
            case AcceptsMp3() => ???
          }
        }
        //#extract_custom_accept_type

        val requestHtml = FakeRequest().withHeaders(ACCEPT -> "audio/mp3")
        assertAction(list, OK, requestHtml)(r => contentAsString(r) === "ok")

      }

    }

    def assertAction[A, T: AsResult](action: Action[A], expectedResponse: Int = OK, request: Request[A] = FakeRequest())(assertions: Future[Result] => T) = {
      running(FakeApplication()) {
        val result = action(request)
        status(result) must_== expectedResponse
        assertions(result)
      }
    }

    object Item {
      def findAll = List(1, 2, 3)
    }

  }
}

package views.html {
  object list {
    def apply(items: Seq[Int]) = items.mkString("<html>", ",", "</html>")
  }
}
