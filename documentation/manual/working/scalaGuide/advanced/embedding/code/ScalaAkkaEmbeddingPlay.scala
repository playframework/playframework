/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import org.specs2.mutable.Specification
import play.api.NoHttpFiltersComponents
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
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/hello/$to") =>
            Action {
              Ok(s"Hello $to")
            }
        }
      }

      try {
        testRequest(9000)
      } finally {
        //#stop-akka-http
        server.stop()
        //#stop-akka-http
      }
    }
    //#simple-akka-http

    "be configurable with akka" in {
      //#config-akka-http
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.AkkaHttpServer
      import play.core.server._

      val server = AkkaHttpServer.fromRouterWithComponents(
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
      //#config-akka-http

      try {
        testRequest(19000)
      } finally {
        server.stop()
      }
    }

    "allow overriding components" in {
      //#components-akka-http
      import play.api.BuiltInComponents
      import play.api.http.DefaultHttpErrorHandler
      import play.api.mvc._
      import play.api.routing.Router
      import play.api.routing.sird._
      import play.core.server.AkkaHttpServerComponents

      import scala.concurrent.Future

      val components = new AkkaHttpServerComponents with BuiltInComponents with NoHttpFiltersComponents {

        override lazy val router: Router = Router.from {
          case GET(p"/hello/$to") =>
            Action {
              Results.Ok(s"Hello $to")
            }
        }

        override lazy val httpErrorHandler = new DefaultHttpErrorHandler(
          environment,
          configuration,
          sourceMapper,
          Some(router)
        ) {

          protected override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
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
      import play.api.inject.guice.GuiceApplicationBuilder
      import play.api.mvc._
      import play.api.routing.SimpleRouterImpl
      import play.api.routing.sird._
      import play.core.server.AkkaHttpServer
      import play.core.server.ServerConfig

      val server = AkkaHttpServer.fromApplication(
        GuiceApplicationBuilder()
          .router(new SimpleRouterImpl({
            case GET(p"/hello/$to") =>
              Action {
                Results.Ok(s"Hello $to")
              }
          }))
          .build(),
        ServerConfig(
          port = Some(19000),
          address = "127.0.0.1"
        )
      )
      //#config-akka-http

      try {
        testRequest(19000)
      } finally {
        server.stop()
      }
      //#application-akka-http
    }

  }

  def testRequest(port: Int) = {
    withClient { client =>
      Await.result(client.url("/hello/world").get(), Duration.Inf).body must_== "Hello world"
    }(new play.api.http.Port(port))
  }
}
