/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

import javax.inject.Provider
import play._
import play.api.mvc.RequestHeader
import play.api.test.ApplicationFactories
import play.api.test.ApplicationFactory
import play.api.test.PlaySpecification
import play.api.OptionalSourceMapper
import play.api.{ Application => ScalaApplication }
import play.core.BuildLink
import play.core.HandleWebCommandSupport
import play.core.SourceMapper
import play.http.HttpErrorHandler
import play.it.test.EndpointIntegrationSpecification
import play.it.test.OkHttpEndpointSupport
import play.mvc.EssentialAction
import play.mvc.EssentialFilter
import play.mvc.Http
import play.mvc.Result
import play.routing.RequestFunctions
import play.routing.RoutingDslComponents

class JavaHttpErrorHandlingSpec
    extends PlaySpecification
    with EndpointIntegrationSpecification
    with ApplicationFactories
    with OkHttpEndpointSupport {
  def createApplicationFactory(
      applicationContext: ApplicationLoader.Context,
      webCommandHandler: Option[HandleWebCommandSupport],
      filters: Seq[EssentialFilter]
  ): ApplicationFactory = new ApplicationFactory {
    override def create(): ScalaApplication = {
      val components = new BuiltInComponentsFromContext(applicationContext) with RoutingDslComponents {
        import scala.collection.JavaConverters._
        import scala.compat.java8.OptionConverters

        // Add the web command handler if it is available
        webCommandHandler.foreach(webCommands().addHandler)

        override def httpFilters(): util.List[mvc.EssentialFilter] = filters.asJava

        override def router(): routing.Router = {
          routingDsl()
            .GET("/")
            .routingTo(new RequestFunctions.Params0[play.mvc.Result] {
              override def apply(t: Http.Request): mvc.Result = play.mvc.Results.ok("Done!")
            })
            .GET("/error")
            .routingTo(new RequestFunctions.Params0[play.mvc.Result] {
              override def apply(t: Http.Request): mvc.Result = throw new RuntimeException("action exception!")
            })
            .build()
        }

        //  Config config, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes
        override def httpErrorHandler(): HttpErrorHandler = {
          val mapper = OptionConverters.toScala(applicationContext.devContext()).map(_.sourceMapper)

          val routesProvider: Provider[play.api.routing.Router] = new Provider[play.api.routing.Router] {
            override def get(): play.api.routing.Router = router().asScala()
          }

          new play.http.DefaultHttpErrorHandler(
            this.config(),
            this.environment(),
            new OptionalSourceMapper(mapper),
            routesProvider
          ) {
            override def onClientError(
                request: Http.RequestHeader,
                statusCode: Int,
                message: String
            ): CompletionStage[Result] = {
              CompletableFuture.completedFuture(mvc.Results.internalServerError(message))
            }

            override def onServerError(request: Http.RequestHeader, exception: Throwable): CompletionStage[Result] = {
              exception match {
                case ite: InvocationTargetException =>
                  CompletableFuture.completedFuture(
                    mvc.Results.internalServerError(s"got exception: ${exception.getCause.getMessage}")
                  )
                case rex: Throwable =>
                  CompletableFuture.completedFuture(
                    mvc.Results.internalServerError(s"got exception: ${exception.getMessage}")
                  )
              }
            }
          }
        }
      }

      components.application().asScala()
    }
  }

  "The configured HttpErrorHandler" should {
    val appFactory: ApplicationFactory = createApplicationFactory(
      applicationContext = new ApplicationLoader.Context(Environment.simple()),
      webCommandHandler = None,
      filters = Seq(
        new EssentialFilter {
          def apply(next: EssentialAction) = {
            throw new RuntimeException("filter exception!")
          }
        }
      )
    )

    val appFactoryWithoutFilters: ApplicationFactory = createApplicationFactory(
      applicationContext = new ApplicationLoader.Context(Environment.simple()),
      webCommandHandler = None,
      filters = Seq.empty
    )

    "handle exceptions that happen in action" in appFactoryWithoutFilters.withAllOkHttpEndpoints { endpoint =>
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

      val scalaApplicationContext = play.api.ApplicationLoader.Context.create(
        environment = play.api.Environment.simple(mode = play.api.Mode.Dev),
        devContext = Some(play.api.ApplicationLoader.DevContext(devSourceMapper, buildLink))
      )

      val applicationContext = new ApplicationLoader.Context(scalaApplicationContext)

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
            override def handleWebCommand(
                request: RequestHeader,
                buildLink: BuildLink,
                path: File
            ): Option[api.mvc.Result] = {
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
