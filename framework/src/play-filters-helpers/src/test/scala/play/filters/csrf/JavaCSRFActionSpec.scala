/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import play.api.mvc.Session
import play.filters.csrf.CSRFConf._
import play.mvc.Http.Context
import play.api.inject.NewInstanceInjector
import play.http.DefaultHttpRequestHandler

import scala.concurrent.Future
import play.api.libs.ws._
import play.mvc.{ Results, Result, Controller }
import play.core.j.{ JavaHandlerComponents, JavaActionAnnotations, JavaAction }
import play.libs.F

/**
 * Specs for the Java per action CSRF actions
 */
object JavaCSRFActionSpec extends CSRFCommonSpecs {

  val javaHandlerComponents = new JavaHandlerComponents(NewInstanceInjector, new DefaultHttpRequestHandler())

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => new JavaAction(javaHandlerComponents) {
        def parser = annotations.parser
        def invocation = F.Promise.pure(if (sendUnauthorizedResult) {
          new MyUnauthorizedAction().check()
        } else {
          new MyAction().check()
        })
        val annotations = if (sendUnauthorizedResult) {
          new JavaActionAnnotations(classOf[MyUnauthorizedAction], classOf[MyUnauthorizedAction].getMethod("check"))
        } else {
          new JavaActionAnnotations(classOf[MyAction], classOf[MyAction].getMethod("check"))
        }
      }
    } {
      import play.api.Play.current
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => new JavaAction(javaHandlerComponents) {
        def parser = annotations.parser
        def invocation = F.Promise.pure(new MyAction().add())
        val annotations = new JavaActionAnnotations(classOf[MyAction], classOf[MyAction].getMethod("add"))
      }
    } {
      import play.api.Play.current
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfWithSession(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => new JavaAction(javaHandlerComponents) {
        def parser = annotations.parser
        def invocation = F.Promise.pure(new MyAction().withSession())
        val annotations = new JavaActionAnnotations(classOf[MyAction], classOf[MyAction].getMethod("withSession"))
      }
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
  }

  class MyAction extends Controller {
    @AddCSRFToken
    def add(): Result = {
      // Simulate a template that adds a CSRF token
      import play.core.j.PlayMagicForJava.requestHeader
      import CSRF.Token.getToken
      Results.ok(implicitly[CSRF.Token].value)
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
    def handle(msg: String) = {
      Results.unauthorized(msg)
    }
  }
}
