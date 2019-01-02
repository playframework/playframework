/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.routing

import java.util.function.Supplier

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAll
import play.{ BuiltInComponents => JBuiltInComponents }
import play.api.Mode
import play.api.routing.Router
import play.it.http.{ BasicHttpClient, BasicRequest }
import play.mvc.{ Result, Results }
import play.routing.RoutingDsl
import play.server.Server
import play.{ Mode => JavaMode }
import scala.compat.java8.FunctionConverters._

class AkkaHTTPServerSpec extends ServerSpec {
  override def serverProvider: String = "play.core.server.AkkaHttpServerProvider"
}

class NettyServerSpec extends ServerSpec {
  override def serverProvider: String = "play.core.server.NettyServerProvider"
}

trait ServerSpec extends Specification with BeforeAll {

  sequential

  def serverProvider: String

  override def beforeAll(): Unit = {
    System.setProperty("play.server.provider", serverProvider)
  }

  private def withServer[T](server: Server)(block: Server => T): T = {
    try {
      block(server)
    } finally {
      server.stop()
    }
  }

  "Java Server" should {

    "start server" in {
      "with default mode and free port" in {
        withServer(
          Server.forRouter(asJavaFunction((components: JBuiltInComponents) => Router.empty.asJava))
        ) { server =>
            server.httpPort() must beGreaterThan(0)
            server.underlying().mode must beEqualTo(Mode.Test)
          }
      }
      "with given port and default mode" in {
        withServer(
          Server.forRouter(9999, asJavaFunction((components: JBuiltInComponents) => Router.empty.asJava))
        ) { server =>
            server.httpPort() must beEqualTo(9999)
            server.underlying().mode must beEqualTo(Mode.Test)
          }
      }
      "with the given mode and free port" in {
        withServer(
          Server.forRouter(JavaMode.DEV, asJavaFunction((components: JBuiltInComponents) => Router.empty.asJava))
        ) { server =>
            server.httpPort() must beGreaterThan(0)
            server.underlying().mode must beEqualTo(Mode.Dev)
          }
      }
      "with the given mode and port" in {
        withServer(
          Server.forRouter(JavaMode.DEV, 9999, asJavaFunction((components: JBuiltInComponents) => Router.empty.asJava))
        ) { server =>
            server.httpPort() must beEqualTo(9999)
            server.underlying().mode must beEqualTo(Mode.Dev)
          }
      }
      "with the given router" in {
        withServer(
          Server.forRouter(JavaMode.DEV, asJavaFunction { components: JBuiltInComponents =>
            RoutingDsl.fromComponents(components)
              .GET("/something").routeTo(
                new Supplier[Result] {
                  override def get() = Results.ok("You got something")
                }
              ).build()
          })
        ) { server =>
            server.underlying().mode must beEqualTo(Mode.Dev)

            val request = BasicRequest("GET", "/something", "HTTP/1.1", Map(), "")
            val responses = BasicHttpClient.makeRequests(port = server.httpPort())(request)
            responses.head.body must beLeft("You got something")
          }
      }
    }

    "get the address the server is running" in {
      withServer(
        Server.forRouter(9999, asJavaFunction((components: JBuiltInComponents) => Router.empty.asJava))
      ) { server =>
          server.mainAddress().getPort must beEqualTo(9999)
        }
    }
  }

}
