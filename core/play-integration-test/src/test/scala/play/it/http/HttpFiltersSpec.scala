/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import scala.concurrent.Future

import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.routing.Router
import play.api.test.ApplicationFactories
import play.api.test.ApplicationFactory
import play.api.test.PlaySpecification
import play.api.Application
import play.api.ApplicationLoader
import play.api.BuiltInComponentsFromContext
import play.api.Environment
import play.it.test.EndpointIntegrationSpecification
import play.it.test.OkHttpEndpointSupport

class HttpFiltersSpec
    extends PlaySpecification
    with EndpointIntegrationSpecification
    with ApplicationFactories
    with OkHttpEndpointSupport {
  "Play http filters" should {
    val appFactory: ApplicationFactory = new ApplicationFactory {
      override def create(): Application = {
        val components = new BuiltInComponentsFromContext(ApplicationLoader.Context.create(Environment.simple())) {
          import play.api.mvc.Results._
          import play.api.routing.sird
          import play.api.routing.sird._
          override lazy val router: Router = Router.from {
            case sird.GET(p"/")        => Action { Ok("Done!") }
            case sird.GET(p"/error")   => Action { Ok("Done!") }
            case sird.GET(p"/invalid") => Action { Ok("Done!") }
          }
          override lazy val httpFilters: Seq[EssentialFilter] = Seq(
            // A non-essential filter that throws an exception
            new Filter {
              override def mat                                                                          = materializer
              override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
                if (rh.path.contains("invalid")) {
                  throw new RuntimeException("INVALID")
                }
                f(rh)
              }
            },
            new EssentialFilter {
              // an essential filter returning an action that throws before returning an accumulator
              def apply(next: EssentialAction) = EssentialAction { rh =>
                if (rh.path.contains("error")) {
                  throw new RuntimeException("ERROR")
                }
                next(rh)
              }
            }
          )

          override lazy val httpErrorHandler: HttpErrorHandler = new HttpErrorHandler {
            override def onServerError(request: RequestHeader, exception: Throwable) = {
              Future(InternalServerError(exception.getMessage))
            }
            override def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
              Future(InternalServerError(message))
            }
          }
        }
        components.application
      }
    }

    "send exceptions from Filters to the HttpErrorHandler" in appFactory.withAllOkHttpEndpoints { endpoint =>
      val request = new okhttp3.Request.Builder()
        .url(endpoint.endpoint.pathUrl("/error"))
        .get()
        .build()
      val response = endpoint.client.newCall(request).execute()
      response.code must_== 500
      response.body.string must_== "ERROR"
    }

    "send exceptions from EssentialFilters to the HttpErrorHandler" in appFactory.withAllOkHttpEndpoints { endpoint =>
      val request = new okhttp3.Request.Builder()
        .url(endpoint.endpoint.pathUrl("/invalid"))
        .get()
        .build()
      val response = endpoint.client.newCall(request).execute()
      response.code must_== 500
      response.body.string must_== "INVALID"
    }
  }
}
