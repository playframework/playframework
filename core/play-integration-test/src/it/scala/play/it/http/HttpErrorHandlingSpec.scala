/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.io.File
import java.util

import play.api.http.DefaultHttpErrorHandler
import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.routing.Router
import play.api.test.ApplicationFactories
import play.api.test.ApplicationFactory
import play.api.test.PlaySpecification
import play.api._
import play.core.BuildLink
import play.core.HandleWebCommandSupport
import play.core.SourceMapper
import play.it.test.EndpointIntegrationSpecification
import play.it.test.OkHttpEndpointSupport

import scala.concurrent.Future

class HttpErrorHandlingSpec
    extends PlaySpecification
    with EndpointIntegrationSpecification
    with ApplicationFactories
    with OkHttpEndpointSupport {
  def createApplicationFactory(
      applicationContext: ApplicationLoader.Context,
      webCommandHandler: Option[HandleWebCommandSupport],
      filters: Seq[EssentialFilter]
  ): ApplicationFactory = new ApplicationFactory {
    override def create(): Application = {
      val components = new BuiltInComponentsFromContext(applicationContext) {
        // Add the web command handler if it is available
        webCommandHandler.foreach(super.webCommands.addHandler)

        import play.api.mvc.Results._
        import play.api.routing.sird
        import play.api.routing.sird._
        override lazy val router: Router = Router.from {
          case sird.GET(p"/error") => throw new RuntimeException("action exception!")
          case sird.GET(p"/")      => Action { Ok("Done!") }
        }

        override def httpFilters: Seq[EssentialFilter] = filters

        override lazy val httpErrorHandler: HttpErrorHandler = new DefaultHttpErrorHandler(
          sourceMapper = applicationContext.devContext.map(_.sourceMapper),
          router = Some(router)
        ) {
          override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
            Future.successful(InternalServerError(message))
          }

          override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
            Future.successful(InternalServerError(s"got exception: ${exception.getMessage}"))
          }
        }
      }
      components.application
    }
  }

  "The configured HttpErrorHandler" should {
    val appFactory: ApplicationFactory = createApplicationFactory(
      applicationContext = ApplicationLoader.Context.create(Environment.simple()),
      webCommandHandler = None,
      filters = Seq(
        new EssentialFilter {
          def apply(next: EssentialAction) = {
            throw new RuntimeException("filter exception!")
          }
        }
      )
    )

    "handle exceptions that happen in action" in appFactory.withAllOkHttpEndpoints { endpoint =>
      val request = new okhttp3.Request.Builder()
        .url(endpoint.endpoint.pathUrl("/error"))
        .get()
        .build()
      val response = endpoint.client.newCall(request).execute()
      response.code must_== 500
      response.body.string must_== "got exception: action exception!"
    }

    "handle exceptions that happen in filters" in appFactory.withAllOkHttpEndpoints { endpoint =>
      val request = new okhttp3.Request.Builder()
        .url(endpoint.endpoint.pathUrl("/"))
        .get()
        .build()
      val response = endpoint.client.newCall(request).execute()
      response.code must_== 500
      response.body.string must_== "got exception: filter exception!"
    }

    "in DEV mode" in {
      val buildLink = new BuildLink {
        override def reload(): AnyRef                                            = null
        override def findSource(className: String, line: Integer): Array[AnyRef] = null
        override def projectPath(): File                                         = new File("").getAbsoluteFile
        override def forceReload(): Unit                                         = { /* do nothing */ }
        override def settings(): util.Map[String, String]                        = util.Collections.emptyMap()
      }

      val devSourceMapper = new SourceMapper {
        override def sourceOf(className: String, line: Option[Int]): Option[(File, Option[Int])] = None
      }

      val applicationContext = ApplicationLoader.Context.create(
        environment = Environment.simple(mode = Mode.Dev),
        devContext = Some(ApplicationLoader.DevContext(devSourceMapper, buildLink))
      )

      val appWithActionException: ApplicationFactory = createApplicationFactory(
        applicationContext = applicationContext,
        webCommandHandler = None,
        filters = Seq.empty
      )

      val appWithFilterException: ApplicationFactory = createApplicationFactory(
        applicationContext = applicationContext,
        webCommandHandler = None,
        filters = Seq(
          new EssentialFilter {
            def apply(next: EssentialAction) = {
              throw new RuntimeException("filter exception!")
            }
          }
        )
      )

      val appWithWebCommandExceptions: ApplicationFactory = createApplicationFactory(
        applicationContext = applicationContext,
        webCommandHandler = Some(
          new HandleWebCommandSupport {
            override def handleWebCommand(request: RequestHeader, buildLink: BuildLink, path: File): Option[Result] = {
              throw new RuntimeException("webcommand exception!")
            }
          }
        ),
        Seq.empty
      )

      "handle exceptions that happens in action" in appWithActionException.withAllOkHttpEndpoints { endpoint =>
        val request = new okhttp3.Request.Builder()
          .url(endpoint.endpoint.pathUrl("/error"))
          .get()
          .build()
        val response = endpoint.client.newCall(request).execute()
        response.code must_== 500
        response.body.string must_== "got exception: action exception!"
      }

      "handle exceptions that happens in filters" in appWithFilterException.withAllOkHttpEndpoints { endpoint =>
        val request = new okhttp3.Request.Builder()
          .url(endpoint.endpoint.pathUrl("/"))
          .get()
          .build()
        val response = endpoint.client.newCall(request).execute()
        response.code must_== 500
        response.body.string must_== "got exception: filter exception!"
      }

      "handle exceptions that happens in web command" in appWithWebCommandExceptions.withAllOkHttpEndpoints {
        endpoint =>
          val request = new okhttp3.Request.Builder()
            .url(endpoint.endpoint.pathUrl("/"))
            .get()
            .build()
          val response = endpoint.client.newCall(request).execute()
          response.code must_== 500
          response.body.string must_== "got exception: webcommand exception!"
      }
    }
  }
}
