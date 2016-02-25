/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csrf

import java.util.concurrent.CompletableFuture
import javax.inject.Inject

import play.api.Play
import play.api.libs.crypto.CSRFTokenSigner
import play.api.libs.ws._
import play.api.mvc.Session
import play.core.j.{ JavaAction, JavaActionAnnotations, JavaHandlerComponents }
import play.core.routing.HandlerInvokerFactory
import play.mvc.Http.{ Context, RequestHeader }
import play.mvc.{ Controller, Result, Results }

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Specs for the Java per action CSRF actions
 */
object JavaCSRFActionSpec extends CSRFCommonSpecs {

  def javaHandlerComponents = Play.privateMaybeApplication.get.injector.instanceOf[JavaHandlerComponents]
  def myAction = Play.privateMaybeApplication.get.injector.instanceOf[MyAction]
  def ws = Play.privateMaybeApplication.get.injector.instanceOf[WSClient]
  //def crypto = Play.privateMaybeApplication.get.injector.instanceOf[CSRFTokenSigner]

  def javaAction[T: ClassTag](method: String, inv: => Result) = new JavaAction(javaHandlerComponents) {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    def parser = HandlerInvokerFactory.javaBodyParserToScala(javaHandlerComponents.getBodyParser(annotations.parser))
    def invocation = CompletableFuture.completedFuture(inv)
    val annotations = new JavaActionAnnotations(clazz, clazz.getMethod(method))
  }

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ if sendUnauthorizedResult => javaAction[MyUnauthorizedAction]("check", new MyUnauthorizedAction().check())
      case _ => javaAction[MyAction]("check", myAction.check())
    } {
      handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => javaAction[MyAction]("add", myAction.add())
    } {
      handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfWithSession(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => javaAction[MyAction]("withSession", myAction.withSession())
    } {
      import play.api.Play.current
      handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
    }
  }

  "The Java CSRF filter support" should {
    "allow adding things to the session when a token is also added to the session" in {
      buildCsrfWithSession()(_.get()) { response =>
        val session = response.cookies.find(_.name.exists(_ == Session.COOKIE_NAME)).flatMap(_.value).map(Session.decode)
        session must beSome.which { s =>
          s.get(TokenName) must beSome[String]
          s.get("hello") must beSome("world")
        }
      }
    }
    "allow accessing the token from the http context" in withServer(Seq(
      "play.http.filters" -> "play.filters.csrf.CsrfFilters"
    )) {
      case _ => javaAction[MyAction]("getToken", myAction.getToken())
    } {
      lazy val token = crypto.generateSignedToken
      import play.api.Play.current
      val returned = await(ws.url("http://localhost:" + testServerPort).withSession(TokenName -> token).get()).body
      crypto.compareSignedTokens(token, returned) must beTrue
    }
  }

  class MyAction extends Controller {
    @AddCSRFToken
    def add(): Result = {
      // Simulate a template that adds a CSRF token
      import play.core.j.PlayMagicForJava.requestHeader
      Results.ok(CSRF.getToken.get.value)
    }
    def getToken(): Result = {
      Results.ok(Option(CSRF.getToken(Controller.request()).orElse(null)) match {
        case Some(CSRF.Token(_, value)) => value
        case None => ""
      })
    }
    @RequireCSRFCheck
    def check(): Result = {
      Results.ok()
    }
    @AddCSRFToken
    def withSession(): Result = {
      Context.current().session().put("hello", "world")
      Results.ok()
    }
  }

  class MyUnauthorizedAction() extends Controller {
    @AddCSRFToken
    def add(): Result = {
      // Simulate a template that adds a CSRF token
      import play.core.j.PlayMagicForJava.requestHeader
      Results.ok(CSRF.getToken.get.value)
    }
    @RequireCSRFCheck(error = classOf[CustomErrorHandler])
    def check(): Result = {
      Results.ok()
    }
  }

  class CustomErrorHandler extends CSRFErrorHandler {
    def handle(req: RequestHeader, msg: String) = {
      CompletableFuture.completedFuture(Results.unauthorized(msg))
    }
  }
}
