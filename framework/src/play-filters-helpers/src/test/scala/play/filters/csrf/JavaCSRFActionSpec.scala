package play.filters.csrf

import play.api.libs.ws.WS.WSRequestHolder
import play.api.mvc.Session
import play.filters.csrf.CSRFConf._
import play.mvc.Http.Context
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

  def buildCsrfWithSession(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[Response])(handleResponse: (Response) => T) = withServer(configuration) {
      case _ => new JavaAction() {
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

}
