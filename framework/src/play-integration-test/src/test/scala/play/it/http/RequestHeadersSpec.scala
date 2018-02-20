/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import org.specs2.specification.core.Fragments
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.{ Configuration, Mode }
import play.core.server.ServerConfig
import play.it._

class NettyRequestHeadersSpec extends RequestHeadersSpec with NettyIntegrationSpecification

class AkkaHttpRequestHeadersSpec extends RequestHeadersSpec with AkkaHttpIntegrationSpecification

trait RequestHeadersSpec extends PlaySpecification with ServerIntegrationSpecification with HttpHeadersCommonSpec {

  sequential

  def withServerAndConfig[T](configuration: (String, Any)*)(action: (DefaultActionBuilder, PlayBodyParsers) => EssentialAction)(block: Port => T) = {
    val port = testServerPort

    val serverConfig: ServerConfig = {
      val c = ServerConfig(port = Some(testServerPort), mode = Mode.Test)
      c.copy(configuration = c.configuration ++ Configuration(configuration: _*))
    }
    running(play.api.test.TestServer(serverConfig, GuiceApplicationBuilder().appRoutes { app =>
      val Action = app.injector.instanceOf[DefaultActionBuilder]
      val parse = app.injector.instanceOf[PlayBodyParsers]
      ({
        case _ => action(Action, parse)
      })
    }.build(), Some(integrationServerProvider))) {
      block(port)
    }
  }

  def withServer[T](action: (DefaultActionBuilder, PlayBodyParsers) => EssentialAction)(block: Port => T) = {
    withServerAndConfig()(action)(block)
  }

  "Play request header handling" should {

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

    "not expose a content-type when there's no body" in withServer((Action, _) => Action { rh =>
      // the body is a String representation of `get("Content-Type")`
      Results.Ok(rh.headers.get("Content-Type").getOrElse("no-header"))
    }) { port =>
      val Seq(response) = BasicHttpClient.makeRequests(port)(
        // an empty body implies no parsing is used and no content type is derived from the body.
        BasicRequest("GET", "/", "HTTP/1.1", Map.empty, "")
      )
      response.body.left.toOption must beSome("no-header")
    }

    "pass common tests for headers" in withServer((Action, _) => Action { rh =>
      commonTests(rh.headers)
      Results.Ok("Done")
    }) { port =>
      val Seq(response) = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map("a" -> "a2", "a" -> "a1", "b" -> "b3", "b" -> "b2", "B" -> "b1", "c" -> "c1"), "")
      )
      response.status must_== 200
    }

    "get request headers properly when Content-Encoding is set" in {
      withServer((Action, _) => Action { rh =>
        Results.Ok(
          Seq("Content-Encoding", "Authorization", "X-Custom-Header").map { headerName =>
            s"$headerName -> ${rh.headers.get(headerName)}"
          }.mkString(", ")
        )
      }) { port =>
        val Seq(response) = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(
            "Content-Encoding" -> "gzip",
            "Authorization" -> "Bearer 123",
            "X-Custom-Header" -> "123"
          ), "")
        )
        response.body must beLeft(
          "Content-Encoding -> None, " +
            "Authorization -> Some(Bearer 123), " +
            "X-Custom-Header -> Some(123)"
        )
      }
    }
  }
}
