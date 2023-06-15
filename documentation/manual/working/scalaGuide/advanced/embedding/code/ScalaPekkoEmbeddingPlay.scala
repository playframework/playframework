/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import scala.concurrent.duration.Duration
import scala.concurrent.Await

import org.specs2.mutable.Specification
import play.api.routing.Router
import play.api.test.WsTestClient

class ScalaPekkoEmbeddingPlay extends Specification with WsTestClient {
  "Embedding play with pekko" should {
    "be very simple" in {
      // #simple-pekko-http
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.PekkoHttpServer

      val server = PekkoHttpServer.fromRouterWithComponents() { components =>
        import Results._
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/hello/$to") =>
            Action {
              Ok(s"Hello $to")
            }
        }
      }
      // #simple-pekko-http

      try {
        testRequest(9000)
      } finally {
        // #stop-pekko-http
        server.stop()
        // #stop-pekko-http
      }
    }

    "be configurable with pekko" in {
      // #config-pekko-http
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.PekkoHttpServer
      import play.core.server._

      val server = PekkoHttpServer.fromRouterWithComponents(
        ServerConfig(
          port = Some(19000),
          address = "127.0.0.1"
        )
      ) { components =>
        import Results._
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/hello/$to") =>
            Action {
              Ok(s"Hello $to")
            }
        }
      }
      // #config-pekko-http

      try {
        testRequest(19000)
      } finally {
        server.stop()
      }
    }

    "allow overriding components" in {
      // #components-pekko-http
      import play.api.http.DefaultHttpErrorHandler
      import play.api.mvc._
      import play.api.routing.Router
      import play.api.routing.sird._
      import play.core.server.DefaultPekkoHttpServerComponents

      import scala.concurrent.Future

      val components = new DefaultPekkoHttpServerComponents {
        override lazy val router: Router = Router.from {
          case GET(p"/hello/$to") =>
            Action {
              Results.Ok(s"Hello $to")
            }
        }

        override lazy val httpErrorHandler: DefaultHttpErrorHandler = new DefaultHttpErrorHandler(
          environment,
          configuration,
          devContext.map(_.sourceMapper),
          Some(router)
        ) {
          protected override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
            Future.successful(Results.NotFound("Nothing was found!"))
          }
        }
      }
      val server = components.server
      // #components-pekko-http

      try {
        testRequest(9000)
      } finally {
        server.stop()
      }
    }

    "allow usage from a running application" in {
      // #application-pekko-http
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.PekkoHttpServer
      import play.core.server.ServerConfig
      import play.filters.HttpFiltersComponents
      import play.api.Environment
      import play.api.ApplicationLoader
      import play.api.BuiltInComponentsFromContext

      val context = ApplicationLoader.Context.create(Environment.simple())
      val components = new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case GET(p"/hello/$to") =>
            Action {
              Results.Ok(s"Hello $to")
            }
        }
      }

      val server = PekkoHttpServer.fromApplication(
        components.application,
        ServerConfig(
          port = Some(19000),
          address = "127.0.0.1"
        )
      )
      // #application-pekko-http

      try {
        testRequest(19000)
      } finally {
        server.stop()
      }
    }

    "allow usage from with logger configurator" in {
      // #logger-pekko-http
      import play.api.mvc._
      import play.api.routing.sird._
      import play.filters.HttpFiltersComponents
      import play.core.server.PekkoHttpServer
      import play.core.server.ServerConfig
      import play.api.Environment
      import play.api.ApplicationLoader
      import play.api.LoggerConfigurator
      import play.api.BuiltInComponentsFromContext

      val context = ApplicationLoader.Context.create(Environment.simple())
      // Do the logging configuration
      LoggerConfigurator(context.environment.classLoader).foreach {
        _.configure(context.environment, context.initialConfiguration, Map.empty)
      }

      val components = new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case GET(p"/hello/$to") =>
            Action {
              Results.Ok(s"Hello $to")
            }
        }
      }

      val server = PekkoHttpServer.fromApplication(
        components.application,
        ServerConfig(
          port = Some(19000),
          address = "127.0.0.1"
        )
      )
      // #logger-pekko-http

      try {
        testRequest(19000)
      } finally {
        server.stop()
      }
    }
  }

  def testRequest(port: Int) = {
    import play.api.libs.ws.DefaultBodyReadables.readableAsString
    withClient { client => Await.result(client.url("/hello/world").get(), Duration.Inf).body must_== "Hello world" }(
      new play.api.http.Port(port)
    )
  }
}
