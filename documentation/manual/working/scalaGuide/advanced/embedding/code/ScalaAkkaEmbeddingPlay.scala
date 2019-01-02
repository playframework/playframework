/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

import org.specs2.mutable.Specification
import play.api.routing.Router
import play.api.test.WsTestClient

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ScalaAkkaEmbeddingPlay extends Specification with WsTestClient {

  "Embedding play with akka" should {
    "be very simple" in {
      //#simple-akka-http
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.AkkaHttpServer

      val server = AkkaHttpServer.fromRouterWithComponents() { components =>
        import Results._
        import components.{defaultActionBuilder => Action}
        {
          case GET(p"/hello/$to") => Action {
            Ok(s"Hello $to")
          }
        }
      }
      //#simple-akka-http

      try {
        testRequest(9000)
      } finally {
        //#stop-akka-http
        server.stop()
        //#stop-akka-http
      }
    }

    "be configurable with akka" in {
      //#config-akka-http
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.{AkkaHttpServer, _}

      val server = AkkaHttpServer.fromRouterWithComponents(ServerConfig(
        port = Some(19000),
        address = "127.0.0.1"
      )) { components =>
        import Results._
        import components.{defaultActionBuilder => Action}
        {
          case GET(p"/hello/$to") => Action {
            Ok(s"Hello $to")
          }
        }
      }
      //#config-akka-http

      try {
        testRequest(19000)
      } finally {
        server.stop()
      }
    }

    "allow overriding components" in {
      //#components-akka-http
      import play.api.http.DefaultHttpErrorHandler
      import play.api.mvc._
      import play.api.routing.Router
      import play.api.routing.sird._
      import play.core.server.DefaultAkkaHttpServerComponents

      import scala.concurrent.Future

      val components = new DefaultAkkaHttpServerComponents {

        override lazy val router: Router = Router.from {
          case GET(p"/hello/$to") => Action {
            Results.Ok(s"Hello $to")
          }
        }

        override lazy val httpErrorHandler = new DefaultHttpErrorHandler(
          environment,
          configuration,
          devContext.map(_.sourceMapper),
          Some(router)
        ) {

          override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
            Future.successful(Results.NotFound("Nothing was found!"))
          }
        }
      }
      val server = components.server
      //#components-akka-http

      try {
        testRequest(9000)
      } finally {
        server.stop()
      }
    }

    "allow usage from a running application" in {
      //#application-akka-http
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.{AkkaHttpServer, ServerConfig}
      import play.filters.HttpFiltersComponents
      import play.api.{ Environment, ApplicationLoader, BuiltInComponentsFromContext }

      val context = ApplicationLoader.Context.create(Environment.simple())
      val components = new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case GET(p"/hello/$to") => Action {
            Results.Ok(s"Hello $to")
          }
        }
      }

      val server = AkkaHttpServer.fromApplication(components.application, ServerConfig(
        port = Some(19000),
        address = "127.0.0.1"
      ))
      //#application-akka-http

      try {
        testRequest(19000)
      } finally {
        server.stop()
      }
    }

    "allow usage from with logger configurator" in {
      //#logger-akka-http
      import play.api.mvc._
      import play.api.routing.sird._
      import play.filters.HttpFiltersComponents
      import play.core.server.{AkkaHttpServer, ServerConfig}
      import play.api.{ Environment, ApplicationLoader, LoggerConfigurator, BuiltInComponentsFromContext }

      val context = ApplicationLoader.Context.create(Environment.simple())
      // Do the logging configuration
      LoggerConfigurator(context.environment.classLoader).foreach {
        _.configure(context.environment, context.initialConfiguration, Map.empty)
      }

      val components = new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case GET(p"/hello/$to") => Action {
            Results.Ok(s"Hello $to")
          }
        }
      }

      val server = AkkaHttpServer.fromApplication(components.application, ServerConfig(
        port = Some(19000),
        address = "127.0.0.1"
      ))
      //#logger-akka-http

      try {
        testRequest(19000)
      } finally {
        server.stop()
      }
    }
  }

  def testRequest(port: Int) = {
    withClient { client =>
      Await.result(client.url("/hello/world").get(), Duration.Inf).body must_== "Hello world"
    }(new play.api.http.Port(port))
  }
}
