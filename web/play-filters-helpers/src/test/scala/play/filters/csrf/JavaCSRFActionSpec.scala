/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf

import java.util.concurrent.CompletableFuture

import play.api.Application
import play.api.libs.ws._
import play.core.j.JavaAction
import play.core.j.JavaActionAnnotations
import play.core.j.JavaHandlerComponents
import play.core.routing.HandlerInvokerFactory
import play.http.HttpErrorHandler
import play.mvc.Http.RequestHeader
import play.mvc.Http.{ Request => JRequest }
import play.mvc.Controller
import play.mvc.Result
import play.mvc.Results

import scala.compat.java8.OptionConverters._
import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Specs for the Java per action CSRF actions
 */
class JavaCSRFActionSpec extends CSRFCommonSpecs {
  def javaHandlerComponents(implicit app: Application) = inject[JavaHandlerComponents]
  def myAction(implicit app: Application)              = inject[JavaCSRFActionSpec.MyAction]

  def javaAction[T: ClassTag](method: String, inv: JRequest => Result)(implicit app: Application) =
    new JavaAction(javaHandlerComponents) {
      val clazz                     = implicitly[ClassTag[T]].runtimeClass
      def parser                    = HandlerInvokerFactory.javaBodyParserToScala(javaHandlerComponents.getBodyParser(annotations.parser))
      def invocation(req: JRequest) = CompletableFuture.completedFuture(inv(req))
      val annotations =
        new JavaActionAnnotations(
          clazz,
          clazz.getMethod(method, classOf[JRequest]),
          handlerComponents.httpConfiguration.actionComposition
        )
    }

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(configuration) { implicit app =>
        {
          case _ if sendUnauthorizedResult =>
            javaAction[JavaCSRFActionSpec.MyUnauthorizedAction](
              "check",
              new JavaCSRFActionSpec.MyUnauthorizedAction().check
            )
          case _ =>
            javaAction[JavaCSRFActionSpec.MyAction]("check", myAction.check)
        }
      } { ws =>
        handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
      }
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(configuration) { implicit app =>
        { case _ => javaAction[JavaCSRFActionSpec.MyAction]("add", myAction.add) }
      } { ws =>
        handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
      }
    }
  }

  def buildCsrfWithSession(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(configuration) { implicit app =>
        { case _ => javaAction[JavaCSRFActionSpec.MyAction]("withSession", myAction.withSession) }
      } { ws =>
        handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
      }
    }
  }

  "The Java CSRF filter support" should {
    "allow adding things to the session when a token is also added to the session" in {
      buildCsrfWithSession()(_.get()) { response =>
        val session = response.cookie(sessionCookieBaker.COOKIE_NAME).map(_.value).map(sessionCookieBaker.decode)
        session must beSome.which { s =>
          s.get(TokenName) must beSome[String]
          s.get("hello") must beSome("world")
        }
      }
    }
    "allow accessing the token from the http request" in withActionServer(
      Seq(
        "play.http.filters" -> "play.filters.csrf.CsrfFilters"
      )
    ) { implicit app =>
      { case _ => javaAction[JavaCSRFActionSpec.MyAction]("getToken", myAction.getToken) }
    } { ws =>
      lazy val token = signedTokenProvider.generateToken
      val returned   = await(ws.url("http://localhost:" + testServerPort).withSession(TokenName -> token).get()).body
      signedTokenProvider.compareTokens(token, returned) must beTrue
    }
  }
}

object JavaCSRFActionSpec {
  class MyAction extends Controller {
    @AddCSRFToken
    def add(request: JRequest): Result = {
      require(request.asScala() != null) // Make sure request is set
      // Simulate a template that adds a CSRF token
      import play.core.j.PlayMagicForJava._
      implicit val req = request
      Results.ok(CSRF.getToken.get.value)
    }
    def getToken(request: JRequest): Result = {
      Results.ok(Option(CSRF.getToken(request).orElse(null)) match {
        case Some(CSRF.Token(_, value)) => value
        case None                       => ""
      })
    }
    @RequireCSRFCheck
    def check(request: JRequest): Result = {
      Results.ok()
    }
    @AddCSRFToken
    def withSession(request: JRequest): Result = {
      Results.ok().addingToSession(request, "hello", "world")
    }
  }

  class MyUnauthorizedAction() extends Controller {
    @AddCSRFToken
    def add(request: JRequest): Result = {
      // Simulate a template that adds a CSRF token
      import play.core.j.PlayMagicForJava._
      implicit val req = request
      Results.ok(CSRF.getToken.get.value)
    }
    @RequireCSRFCheck(error = classOf[CustomErrorHandler])
    def check(request: JRequest): Result = {
      Results.ok()
    }
  }

  class CustomErrorHandler extends CSRFErrorHandler {
    def handle(req: RequestHeader, msg: String) = {
      CompletableFuture.completedFuture(
        Results.unauthorized(
          "Origin: " + req
            .attrs()
            .getOptional(HttpErrorHandler.Attrs.HTTP_ERROR_INFO)
            .asScala
            .map(_.origin)
            .getOrElse("<not set>") + " / " + msg
        )
      )
    }
  }
}
