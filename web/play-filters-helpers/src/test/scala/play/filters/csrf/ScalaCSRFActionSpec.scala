/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf

import scala.concurrent.Future

import play.api.http.HttpErrorHandler
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSResponse
import play.api.mvc._
import play.api.Application

/**
 * Specs for the Scala per action CSRF actions
 */
class ScalaCSRFActionSpec extends CSRFCommonSpecs {
  def csrfAddToken(app: Application) = app.injector.instanceOf[CSRFAddToken]
  def csrfCheck(app: Application)    = app.injector.instanceOf[CSRFCheck]

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(configuration)(implicit app => {
        case _ =>
          if (sendUnauthorizedResult) {
            val myAction   = inject[DefaultActionBuilder]
            val csrfAction = csrfCheck(app)
            csrfAction(myAction(req => Results.Ok), new CustomErrorHandler())
          } else {
            val myAction   = inject[DefaultActionBuilder]
            val csrfAction = csrfCheck(app)
            csrfAction(myAction(req => Results.Ok))
          }
      }) { (ws, port) => handleResponse(await(makeRequest(ws.url("http://localhost:" + port)))) }
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequest) => Future[WSResponse])(handleResponse: (WSResponse) => T) = {
      withActionServer(configuration)(implicit app => {
        case _ =>
          val myAction   = inject[DefaultActionBuilder]
          val csrfAction = csrfAddToken(app)
          csrfAction(myAction { implicit req =>
            CSRF.getToken
              .map { token => Results.Ok(token.value) }
              .getOrElse(Results.NotFound)
          })
      }) { (ws, port) => handleResponse(await(makeRequest(ws.url("http://localhost:" + port)))) }
    }
  }

  class CustomErrorHandler extends CSRF.ErrorHandler {
    import play.api.mvc.Results.Unauthorized
    def handle(req: RequestHeader, msg: String) =
      Future.successful(
        Unauthorized(
          "Origin: " + req.attrs
            .get(HttpErrorHandler.Attrs.HttpErrorInfo)
            .map(_.origin)
            .getOrElse("<not set>") + " / " + msg
        )
      )
  }
}
