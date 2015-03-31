/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import play.api.libs.Crypto
import play.api.mvc.Session
import play.libs.F.Promise
import play.mvc.Http.{ RequestHeader, Context }

import scala.concurrent.Future
import play.api.libs.ws._
import play.mvc.{ Results, Result, Controller }
import play.core.j.{ JavaHandlerComponents, JavaActionAnnotations, JavaAction }
import play.libs.F

import scala.reflect.ClassTag

/**
 * Specs for the Java per action CSRF actions
 */
object JavaCSRFActionSpec extends CSRFCommonSpecs {

  def javaHandlerComponents = play.api.Play.current.injector.instanceOf[JavaHandlerComponents]

  def javaAction[T: ClassTag](method: String, inv: => Result) = new JavaAction(javaHandlerComponents) {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    def parser = annotations.parser
    def invocation = F.Promise.pure(inv)
    val annotations = new JavaActionAnnotations(clazz, clazz.getMethod(method))
  }

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ if sendUnauthorizedResult => javaAction[MyUnauthorizedAction]("check", new MyUnauthorizedAction().check())
      case _ => javaAction[MyAction]("check", new MyAction().check())
    } {
      import play.api.Play.current
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => javaAction[MyAction]("add", new MyAction().add())
    } {
      import play.api.Play.current
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfWithSession(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => javaAction[MyAction]("withSession", new MyAction().withSession())
    } {
      import play.api.Play.current
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
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
    "allow accessing the token from the http context" in withServer(Nil) {
      case _ => javaAction[MyAction]("getToken", new MyAction().getToken())
    } {
      lazy val token = Crypto.generateSignedToken
      import play.api.Play.current
      val returned = await(WS.url("http://localhost:" + testServerPort).withSession(TokenName -> token).get()).body
      Crypto.compareSignedTokens(token, returned) must beTrue
    }
  }

  class MyAction extends Controller {
    @AddCSRFToken
    def add(): Result = {
      // Simulate a template that adds a CSRF token
      import play.core.j.PlayMagicForJava.requestHeader
      import CSRF.Token.getToken
      Results.ok(implicitly[CSRF.Token].value)
    }
    def getToken(): Result = {
      Results.ok(Option(CSRF.getToken(Controller.request()).orElse(null)) match {
        case Some(CSRF.Token(value)) => value
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

  class MyUnauthorizedAction extends Controller {
    @AddCSRFToken
    def add(): Result = {
      // Simulate a template that adds a CSRF token
      import play.core.j.PlayMagicForJava.requestHeader
      import CSRF.Token.getToken
      Results.ok(implicitly[CSRF.Token].value)
    }
    @RequireCSRFCheck(error = classOf[CustomErrorHandler])
    def check(): Result = {
      Results.ok()
    }
  }

  class CustomErrorHandler extends CSRFErrorHandler {
    def handle(req: RequestHeader, msg: String) = {
      Promise.pure(Results.unauthorized(msg))
    }
  }
}
