/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.embedding

import org.specs2.mutable.Specification
import play.api.test.WsTestClient

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ScalaEmbeddingPlay extends Specification with WsTestClient {

  "Embedding play" should {
    "be very simple" in {

      //#simple
      import play.core.server._
      import play.api.routing.sird._
      import play.api.mvc._

      val server = NettyServer.fromRouter() {
        case GET(p"/hello/$to") => Action {
          Results.Ok(s"Hello $to")
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
      import play.core.server._
      import play.api.routing.sird._
      import play.api.mvc._

      val server = NettyServer.fromRouter(ServerConfig(
        port = Some(19000),
        address = "127.0.0.1"
      )) {
        case GET(p"/hello/$to") => Action {
          Results.Ok(s"Hello $to")
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
      import play.core.server._
      import play.api.routing.Router
      import play.api.routing.sird._
      import play.api.mvc._
      import play.api.BuiltInComponents
      import play.api.http.DefaultHttpErrorHandler
      import scala.concurrent.Future

      val components = new NettyServerComponents with BuiltInComponents {

        lazy val router = Router.from {
          case GET(p"/hello/$to") => Action {
            Results.Ok(s"Hello $to")
          }
        }

        override lazy val httpErrorHandler = new DefaultHttpErrorHandler(environment,
          configuration, sourceMapper, Some(router)) {

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

  }

  def testRequest(port: Int) = {
    withClient { client =>
      Await.result(client.url("/hello/world").get(), Duration.Inf).body must_== "Hello world"
    }(new play.api.http.Port(port))
  }
}
