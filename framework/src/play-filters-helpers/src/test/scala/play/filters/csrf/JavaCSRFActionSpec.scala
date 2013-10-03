package play.filters.csrf

import play.api.libs.ws.WS.WSRequestHolder
import scala.concurrent.Future
import play.api.libs.ws.{WS, Response}
import play.mvc.{Results, Result, Controller}
import play.core.j.{JavaActionAnnotations, JavaAction}
import play.libs.F

/**
 * Specs for the Java per action CSRF actions
 */
object JavaCSRFActionSpec extends CSRFCommonSpecs {

  def buildCsrfCheckRequest(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[Response])(handleResponse: (Response) => T) = withServer(configuration) {
      case _ => new JavaAction() {
        def parser = annotations.parser
        def invocation = F.Promise.pure(new MyAction().check())
        val annotations = new JavaActionAnnotations(classOf[MyAction], classOf[MyAction].getMethod("check"))
      }
    } {
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[Response])(handleResponse: (Response) => T) = withServer(configuration) {
      case _ => new JavaAction() {
        def parser = annotations.parser
        def invocation = F.Promise.pure(new MyAction().add())
        val annotations = new JavaActionAnnotations(classOf[MyAction], classOf[MyAction].getMethod("add"))
      }
    } {
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

}
