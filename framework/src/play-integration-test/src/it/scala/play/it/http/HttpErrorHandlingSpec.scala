/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.routing.Router
import play.api.test.{ ApplicationFactories, ApplicationFactory, PlaySpecification }
import play.api.{ Application, ApplicationLoader, BuiltInComponentsFromContext, Environment }
import play.it.test.{ EndpointIntegrationSpecification, OkHttpEndpointSupport }

import scala.concurrent.Future

class HttpErrorHandlingSpec extends PlaySpecification
  with EndpointIntegrationSpecification with ApplicationFactories with OkHttpEndpointSupport {

  "The configured HttpErrorHandler" should {

    val appFactory: ApplicationFactory = new ApplicationFactory {
      override def create(): Application = {
        val components = new BuiltInComponentsFromContext(
          ApplicationLoader.Context.create(Environment.simple())) {
          import play.api.mvc.Results._
          import play.api.routing.sird
          import play.api.routing.sird._
          override lazy val router: Router = Router.from {
            case sird.GET(p"/error") => throw new RuntimeException("error!")
            case sird.GET(p"/") => Action { Ok("Done!") }
          }
          override lazy val httpFilters: Seq[EssentialFilter] = Seq(
            new EssentialFilter {
              def apply(next: EssentialAction) = {
                throw new RuntimeException("something went wrong!")
              }
            }
          )

          override lazy val httpErrorHandler: HttpErrorHandler = new HttpErrorHandler {
            override def onServerError(request: RequestHeader, exception: Throwable) = {
              Future(InternalServerError(s"got exception: ${exception.getMessage}"))
            }
            override def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
              Future(InternalServerError(message))
            }
          }
        }
        components.application
      }
    }

    "handle exceptions that happen in routing" in appFactory.withAllOkHttpEndpoints { endpoint =>
      val request = new okhttp3.Request.Builder()
        .url(endpoint.endpoint.pathUrl("/error"))
        .get()
        .build()
      val response = endpoint.client.newCall(request).execute()
      response.code must_== 500
      response.body.string must_== "got exception: error!"
    }

    "handle exceptions that happen in filters" in appFactory.withAllOkHttpEndpoints { endpoint =>
      val request = new okhttp3.Request.Builder()
        .url(endpoint.endpoint.pathUrl("/"))
        .get()
        .build()
      val response = endpoint.client.newCall(request).execute()
      response.code must_== 500
      response.body.string must_== "got exception: something went wrong!"
    }
  }
}
