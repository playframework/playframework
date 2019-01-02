/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.advanced.embedding

import org.specs2.mutable.Specification
import play.api.test.WsTestClient

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ScalaNettyEmbeddingPlay extends Specification with WsTestClient {

  "Embedding play" should {
    "be very simple" in {

      //#simple
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server._

      val server = NettyServer.fromRouterWithComponents() { components =>
        import components.{defaultActionBuilder => Action}
        {
          case GET(p"/hello/$to") => Action {
            Results.Ok(s"Hello $to")
          }
        }
      }
      //#simple

      try {
        testRequest(9000)
      } finally {
        //#stop
        server.stop()
        //#stop
      }
    }

    "be configurable" in {
      //#config
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server._

      val server = NettyServer.fromRouterWithComponents(ServerConfig(
        port = Some(19000),
        address = "127.0.0.1"
      )) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case GET(p"/hello/$to") => Action {
            Results.Ok(s"Hello $to")
          }
        }
      }
      //#config

      try {
        testRequest(19000)
      } finally {
        server.stop()
      }
    }

    "allow overriding components" in {
      //#components
      import play.api.http.DefaultHttpErrorHandler
      import play.api.mvc._
      import play.api.routing.Router
      import play.api.routing.sird._
      import play.core.server._

      import scala.concurrent.Future

      val components = new DefaultNettyServerComponents {

        lazy val router = Router.from {
          case GET(p"/hello/$to") => Action {
            Results.Ok(s"Hello $to")
          }
        }

        override lazy val httpErrorHandler = new DefaultHttpErrorHandler(environment,
          configuration, devContext.map(_.sourceMapper), Some(router)) {

          override protected def onNotFound(request: RequestHeader, message: String) = {
            Future.successful(Results.NotFound("Nothing was found!"))
          }
        }
      }
      val server = components.server
      //#components

      try {
        testRequest(9000)
      } finally {
        server.stop()
      }
    }

    "work with logger configurator" in {
      //#logger-configurator
      import play.api.mvc._
      import play.api.routing.Router
      import play.api.routing.sird._
      import play.api._
      import play.core.server._

      val environment = Environment.simple(mode = Mode.Prod)
      val context = ApplicationLoader.Context.create(environment)

      // Do the logging configuration
      LoggerConfigurator(context.environment.classLoader).foreach {
        _.configure(context.environment, context.initialConfiguration, Map.empty)
      }

      val components = new DefaultNettyServerComponents {
        override def router: Router = Router.from {
          case GET(p"/hello/$to") => Action {
            Results.Ok(s"Hello $to")
          }
        }
      }

      val server = components.server
      //#logger-configurator

      try {
        testRequest(9000)
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
