/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.forms.csrf

import play.api.test._
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.Crypto
import play.api.mvc.Call
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

object ScalaCsrf extends PlaySpecification {

  "Play's CSRF protection" should {
    "allow global configuration" in withApplication {
      //#http-filters
      import play.api.http.HttpFilters
      import play.filters.csrf.CSRFFilter
      import javax.inject.Inject

      class Filters @Inject() (csrfFilter: CSRFFilter) extends HttpFilters {
        def filters = Seq(csrfFilter)
      }
      //#http-filters
      ok
    }

    "allow getting the token" in withApplication {
      val originalToken = Crypto.generateSignedToken
      val request = FakeRequest().withSession("csrfToken" -> originalToken)
      //#get-token
      import play.filters.csrf.CSRF

      val token = CSRF.getToken(request)
      //#get-token
      token must beSome.like {
        case t => Crypto.compareSignedTokens(originalToken, t.value) must_== true
      }
    }

    "allow rendering a token in a query string" in withApplication {
      val token = Crypto.generateSignedToken
      val body = scalaguide.forms.html.csrf(FakeRequest().withSession("csrfToken" -> token)).body
      body must find("action=\"/items\\?csrfToken=[a-f0-9]+-\\d+-([a-f0-9]+)\"").withGroup(Crypto.extractSignedToken(token).get)
    }

    "allow rendering a token in a hidden field" in withApplication {
      val token = Crypto.generateSignedToken
      val body = scalaguide.forms.html.csrf(FakeRequest().withSession("csrfToken" -> token)).body
      body must find("value=\"[a-f0-9]+-\\d+-([a-f0-9]+)\"").withGroup(Crypto.extractSignedToken(token).get)
    }

    "allow per action checking" in withApplication {
      import play.api.mvc.Results.Ok
      //#csrf-check
      import play.api.mvc._
      import play.filters.csrf._

      def save = CSRFCheck {
        Action { req =>
          // handle body
          Ok
        }
      }
      //#csrf-check

      await(save(FakeRequest("POST", "/").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")))
        .header.status must_== FORBIDDEN
    }

    "allow per action token handling" in withApplication {
      import play.api.mvc.Results.Ok

      //#csrf-add-token
      import play.api.mvc._
      import play.filters.csrf._

      def form = CSRFAddToken {
        Action { implicit req =>
          Ok(views.html.itemsForm())
        }
      }
      //#csrf-add-token

      val body = await(form(FakeRequest("GET", "/")).flatMap(_.body |>>> Iteratee.consume[Array[Byte]]()))
      Crypto.extractSignedToken(new String(body, "UTF-8")) must beSome
    }

    "be easy to use with an action builder" in withApplication {
      import play.api.mvc.Results.Ok

      //#csrf-action-builder
      import play.api.mvc._
      import play.filters.csrf._

      object PostAction extends ActionBuilder[Request] {
        def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
          // authentication code here
          block(request)
        }
        override def composeAction[A](action: Action[A]) = CSRFCheck(action)
      }

      object GetAction extends ActionBuilder[Request] {
        def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
          // authentication code here
          block(request)
        }
        override def composeAction[A](action: Action[A]) = CSRFAddToken(action)
      }
      //#csrf-action-builder

      //#csrf-actions
      def save = PostAction {
        // handle body
        Ok
      }

      def form = GetAction { implicit req =>
        Ok(views.html.itemsForm())
      }
      //#csrf-actions

      await(save(FakeRequest("POST", "/").withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")))
        .header.status must_== FORBIDDEN
      val body = await(form(FakeRequest("GET", "/")).flatMap(_.body |>>> Iteratee.consume[Array[Byte]]()))
        Crypto.extractSignedToken(new String(body, "UTF-8")) must beSome
    }


  }

  object views {
    object html {
      def itemsForm()(implicit req: play.api.mvc.RequestHeader) =
        play.filters.csrf.CSRF.getToken(req).map(_.value).getOrElse("no token")
    }
  }

  def withApplication[T](block: => T) = running(FakeApplication()) {
    block
  }

}

object routes {
  object ItemsController {
    def save(): Call = Call("POST", "/items")
  }
}
