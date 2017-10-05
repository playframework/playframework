/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.{ Configuration, Mode }
import play.core.server.ServerConfig
import play.it._

class NettyRequestHeadersSpec extends RequestHeadersSpec with NettyIntegrationSpecification
class AkkaHttpRequestHeadersSpec extends RequestHeadersSpec with AkkaHttpIntegrationSpecification

trait RequestHeadersSpec extends PlaySpecification with ServerIntegrationSpecification {

  sequential

  "Play request header handling" should {

    def withServerAndConfig[T](configuration: (String, Any)*)(action: (DefaultActionBuilder, PlayBodyParsers) => EssentialAction)(block: Port => T) = {
      val port = testServerPort

      val serverConfig: ServerConfig = {
        val c = ServerConfig(port = Some(testServerPort), mode = Mode.Test)
        c.copy(configuration = c.configuration ++ Configuration(configuration: _*))
      }
      running(play.api.test.TestServer(serverConfig, GuiceApplicationBuilder().appRoutes { app =>
        val Action = app.injector.instanceOf[DefaultActionBuilder]
        val parse = app.injector.instanceOf[PlayBodyParsers]
        ({ case _ => action(Action, parse) })
      }.build(), Some(integrationServerProvider))) {
        block(port)
      }
    }

    def withServer[T](action: (DefaultActionBuilder, PlayBodyParsers) => EssentialAction)(block: Port => T) = {
      withServerAndConfig()(action)(block)
    }

    "get request headers properly" in withServer((Action, _) => Action { rh =>
      Results.Ok(rh.headers.getAll("Origin").mkString(","))
    }) { port =>
      val Seq(response) = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map("origin" -> "http://foo"), "")
      )
      response.body.left.toOption must beSome("http://foo")
    }

    "remove request headers properly" in withServer((Action, _) => Action { rh =>
      Results.Ok(rh.headers.remove("ORIGIN").getAll("Origin").mkString(","))
    }) { port =>
      val Seq(response) = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map("origin" -> "http://foo"), "")
      )
      response.body.left.toOption must beSome("")
    }

    "replace request headers properly" in withServer((Action, _) => Action { rh =>
      Results.Ok(rh.headers.replace("Origin" -> "https://bar.com").getAll("Origin").mkString(","))
    }) { port =>
      val Seq(response) = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map("origin" -> "http://foo"), "")
      )
      response.body.left.toOption must beSome("https://bar.com")
    }
  }
}
