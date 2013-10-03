package play.filters.csrf

import play.api.libs.ws.WS.WSRequestHolder
import scala.concurrent.Future
import play.api.libs.ws.{WS, Response}
import play.api.mvc._

/**
 * Specs for the Scala per action CSRF actions
 */
object ScalaCSRFActionSpec extends CSRFCommonSpecs {

  def buildCsrfCheckRequest(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[Response])(handleResponse: (Response) => T) = withServer(configuration) {
      case _ => CSRFCheck(Action(Results.Ok))
    } {
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[Response])(handleResponse: (Response) => T) = withServer(configuration) {
      case _ => CSRFAddToken(Action { implicit req =>
        CSRF.getToken(req).map { token =>
          Results.Ok(token.value)
        } getOrElse Results.NotFound
      })
    } {
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }
}
