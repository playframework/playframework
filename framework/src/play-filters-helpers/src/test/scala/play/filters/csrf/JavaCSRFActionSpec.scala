/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import scala.concurrent.Future
import play.api.libs.ws._
import play.mvc.{Results, Result, Controller}
import play.core.j.{JavaActionAnnotations, JavaAction}
import play.libs.F

/**
 * Specs for the Java per action CSRF actions
 */
object JavaCSRFActionSpec extends CSRFCommonSpecs {

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => new JavaAction() {
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
      case _ => new JavaAction() {
        def parser = annotations.parser
        def invocation = F.Promise.pure(new MyAction().add())
        val annotations = new JavaActionAnnotations(classOf[MyAction], classOf[MyAction].getMethod("add"))
      }
    } {
      import play.api.Play.current
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
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
