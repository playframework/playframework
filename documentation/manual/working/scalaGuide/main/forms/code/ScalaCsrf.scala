/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.forms.csrf

import play.api.Application
import javax.inject.Inject
import play.api.test._
import play.api.libs.crypto.CSRFTokenSigner
import play.api.mvc.Call
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

//#csrf-controller
import play.api.mvc._
import play.api.mvc.Results._
import play.filters.csrf._
import play.filters.csrf.CSRF.Token

class CSRFController(components: ControllerComponents, addToken: CSRFAddToken, checkToken: CSRFCheck)
    extends AbstractController(components) {
  def getToken =
    addToken(Action { implicit request =>
      val Token(name, value) = CSRF.getToken.get
      Ok(s"$name=$value")
    })
}
//#csrf-controller

class ScalaCsrf extends PlaySpecification {

  // used to make sure CSRFController gets the proper things injected
  implicit def addToken[A](action: Action[A])(implicit app: Application): Action[A] =
    app.injector.instanceOf(classOf[CSRFAddToken])(action)
  implicit def checkToken[A](action: Action[A])(implicit app: Application): Action[A] =
    app.injector.instanceOf(classOf[CSRFCheck])(action)

  private def Action[A](block: Request[AnyContent] => Result)(implicit app: Application) =
    app.injector.instanceOf(classOf[DefaultActionBuilder]).apply(block)

  "Play's CSRF protection" should {
    "allow global configuration" in new WithApplication() {
      //#http-filters
      import play.api.http.DefaultHttpFilters
      import play.filters.csrf.CSRFFilter
      import javax.inject.Inject

      class Filters @Inject()(csrfFilter: CSRFFilter) extends DefaultHttpFilters(csrfFilter)
      //#http-filters
      ok
    }

    "allow getting the token" in new WithApplication {
      val csrfTokenSigner = app.injector.instanceOf[CSRFTokenSigner]
      val originalToken   = csrfTokenSigner.generateSignedToken
      val addAndGetToken = addToken(Action { implicit request =>
        //#get-token
        val token: Option[CSRF.Token] = CSRF.getToken
        //#get-token
        Ok(token.map(_.value).getOrElse(""))
      })
      val result = addAndGetToken(FakeRequest().withSession("csrfToken" -> originalToken))
      (contentAsString(result) must be).like {
        case t => csrfTokenSigner.compareSignedTokens(originalToken, t) must_== true
      }
    }

    def tokenFormAction(implicit app: Application) =
      addToken(Action { implicit request =>
        Ok(scalaguide.forms.html.csrf())
      })

    "allow rendering a token in a query string" in new WithApplication() {
      val csrfTokenSigner = app.injector.instanceOf[CSRFTokenSigner]
      val originalToken   = csrfTokenSigner.generateSignedToken
      val result          = tokenFormAction(app)(FakeRequest().withSession("csrfToken" -> originalToken))
      val body            = contentAsString(result)
      body must find("action=\"/items\\?csrfToken=[a-f0-9]+-\\d+-([a-f0-9]+)\"").withGroup(
        csrfTokenSigner.extractSignedToken(originalToken).get
      )
    }

    "allow rendering a token in a hidden field" in new WithApplication() {
      val csrfTokenSigner = app.injector.instanceOf[CSRFTokenSigner]
      val originalToken   = csrfTokenSigner.generateSignedToken
      val result          = tokenFormAction(app)(FakeRequest().withSession("csrfToken" -> originalToken))
      val body            = contentAsString(result)
      body must find("value=\"[a-f0-9]+-\\d+-([a-f0-9]+)\"").withGroup(
        csrfTokenSigner.extractSignedToken(originalToken).get
      )
    }

    "allow per action checking" in new WithApplication {
      import play.api.mvc.Results.Ok
      //#csrf-check
      //###insert: import play.api.mvc._
      import play.filters.csrf._

      def save = checkToken {
        Action { implicit req =>
          // handle body
          Ok
        }
      }
      //#csrf-check

      await(
        save(
          FakeRequest("POST", "/")
            .withHeaders("Cookie" -> "foo=bar")
            .withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
        )
      ).header.status must_== FORBIDDEN
    }

    "allow per action token handling" in new WithApplication() {
      import play.api.mvc.Results.Ok
      val csrfTokenSigner = app.injector.instanceOf[CSRFTokenSigner]
      //#csrf-add-token
      //###insert: import play.api.mvc._
      import play.filters.csrf._

      def form = addToken {
        Action { implicit req =>
          Ok(views.html.itemsForm)
        }
      }
      //#csrf-add-token

      val body = await(form(FakeRequest("GET", "/")).flatMap(_.body.consumeData))
      csrfTokenSigner.extractSignedToken(body.utf8String) must beSome
    }

    "be easy to use with an action builder" in new WithApplication() {
      import play.api.mvc.Results.Ok
      val csrfTokenSigner = app.injector.instanceOf[CSRFTokenSigner]
      //#csrf-action-builder
      import play.api.mvc._
      import play.filters.csrf._

      class PostAction @Inject()(parser: BodyParsers.Default) extends ActionBuilderImpl(parser) {
        override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
          // authentication code here
          block(request)
        }
        override def composeAction[A](action: Action[A]) = checkToken(action)
      }

      class GetAction @Inject()(parser: BodyParsers.Default) extends ActionBuilderImpl(parser) {
        override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
          // authentication code here
          block(request)
        }
        override def composeAction[A](action: Action[A]) = addToken(action)
      }
      //#csrf-action-builder

      val getAction  = new GetAction(app.injector.instanceOf[BodyParsers.Default])
      val postAction = new PostAction(app.injector.instanceOf[BodyParsers.Default])

      //#csrf-actions
      def save = postAction {
        // handle body
        Ok
      }

      def form = getAction { implicit req =>
        Ok(views.html.itemsForm)
      }
      //#csrf-actions

      await(
        save(
          FakeRequest("POST", "/")
            .withHeaders("Cookie" -> "foo=bar")
            .withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
        )
      ).header.status must_== FORBIDDEN
      val body = await(form(FakeRequest("GET", "/")).flatMap(_.body.consumeData))
      csrfTokenSigner.extractSignedToken(body.utf8String) must beSome
    }

  }

  object views {
    object html {
      def itemsForm(implicit req: play.api.mvc.RequestHeader) =
        CSRF.getToken.map(_.value).getOrElse("no token")
    }
  }

}

object routes {
  object ItemsController {
    def save(): Call = Call("POST", "/items")
  }
}
