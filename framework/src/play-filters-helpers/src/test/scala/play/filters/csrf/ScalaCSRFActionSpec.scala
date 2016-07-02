/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.csrf

import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.mvc._

import scala.concurrent.Future

/**
 * Specs for the Scala per action CSRF actions
 */
object ScalaCSRFActionSpec extends CSRFCommonSpecs {

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => if (sendUnauthorizedResult) {
        csrfCheck(Action(req => Results.Ok), new CustomErrorHandler())
      } else {
        csrfCheck(Action(req => Results.Ok))
      }
    } { ws =>
      handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = withServer(configuration) {
      case _ => csrfAddToken(Action {
        implicit req =>
          CSRF.getToken.map {
            token =>
              Results.Ok(token.value)
          } getOrElse Results.NotFound
      })
    } { ws =>

      handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
    }
  }

  class CustomErrorHandler extends CSRF.ErrorHandler {
    import play.api.mvc.Results.Unauthorized
    def handle(req: RequestHeader, msg: String) = Future.successful(Unauthorized(msg))
  }
}
