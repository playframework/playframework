/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf

import play.api.Application
import play.api.libs.ws.{ WSClient, WSRequest, WSResponse }
import play.api.mvc._

import scala.concurrent.Future

/**
 * Specs for the Scala per action CSRF actions
 */
class ScalaCSRFActionSpec extends CSRFCommonSpecs {

  def csrfAddToken(app: Application) = app.injector.instanceOf[CSRFAddToken]
  def csrfCheck(app: Application) = app.injector.instanceOf[CSRFCheck]

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(configuration)(implicit app => {
        case _ => if (sendUnauthorizedResult) {
          val myAction = inject[DefaultActionBuilder]
          val csrfAction = csrfCheck(app)
          csrfAction(myAction(req => Results.Ok), new CustomErrorHandler())
        } else {
          val myAction = inject[DefaultActionBuilder]
          val csrfAction = csrfCheck(app)
          csrfAction(myAction(req => Results.Ok))
        }
      }){ ws =>
        handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
      }
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(configuration)(implicit app => {
        case _ =>
          val myAction = inject[DefaultActionBuilder]
          val csrfAction = csrfAddToken(app)
          csrfAction(myAction {
            implicit req =>
              CSRF.getToken.map {
                token =>
                  Results.Ok(token.value)
              } getOrElse Results.NotFound
          })
      }){ ws =>
        handleResponse(await(makeRequest(ws.url("http://localhost:" + testServerPort))))
      }
    }
  }

  class CustomErrorHandler extends CSRF.ErrorHandler {
    import play.api.mvc.Results.Unauthorized
    def handle(req: RequestHeader, msg: String) = Future.successful(Unauthorized(msg))
  }
}
