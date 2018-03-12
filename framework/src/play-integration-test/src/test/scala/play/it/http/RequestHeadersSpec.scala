/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import org.specs2.execute.AsResult
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.{ Configuration, Mode }
import play.core.server.ServerConfig
import play.it._

class NettyRequestHeadersSpec extends RequestHeadersSpec with NettyIntegrationSpecification

class AkkaHttpRequestHeadersSpec extends RequestHeadersSpec with AkkaHttpIntegrationSpecification {

  "Akka HTTP request header handling" should {

    "not change UTF-8 Content-Disposition headers" in {
      withServer((Action, _) => Action { rh =>
        Results.Ok(rh.headers.get("Content-Disposition").toString)
      }) { port =>
        val Seq(response) = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map("Content-Disposition" -> "attachment; filename*=UTF-8''Roget%27s%20Thesaurus.pdf"), "")
        )
        //response.body must beLeft("Some(attachment; filename=\"UTF-8''Roget%27s%20Thesaurus.pdf\")")
        response.body must beLeft("Some(attachment; filename*=UTF-8''Roget%27s%20Thesaurus.pdf)")
      }
    }

    "not complain about invalid User-Agent headers" in {

      // This test modifies the global (!) logger to capture log messages.
      // The test will not be reliable when run concurrently. However, since
      // we're checking for the *absence* of log messages the worst thing
      // that will happen is that the test will pass when it should fail. We
      // should not get spurious failures which would cause our CI testing
      // to fail. I think it's still worth including this test because it
      // will still often report correct failures, even if it's not perfect.

      withServerAndConfig()((Action, _) => Action { rh =>
        Results.Ok(rh.headers.get("User-Agent").toString)
      }) { port =>
        def testAgent[R: AsResult](agent: String) = {
          val (_, logMessages) = LogTester.recordLogEvents {
            val Seq(response) = BasicHttpClient.makeRequests(port)(
              BasicRequest("GET", "/", "HTTP/1.1", Map(
                "User-Agent" -> agent
              ), "")
            )
            response.body must beLeft(s"Some($agent)")
          }
          logMessages.map(_.getFormattedMessage) must not contain (contain(agent))
        }
        // These agent strings come from https://github.com/playframework/playframework/issues/7997
        testAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_3 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Mobile/15A432 [FBAN/FBIOS;FBAV/147.0.0.46.81;FBBV/76961488;FBDV/iPhone8,1;FBMD/iPhone;FBSN/iOS;FBSV/11.0.3;FBSS/2;FBCR/T-Mobile.pl;FBID/phone;FBLC/pl_PL;FBOP/5;FBRV/0]")
        testAgent("Mozilla/5.0 (Linux; Android 7.0; TRT-LX1 Build/HUAWEITRT-LX1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/148.0.0.51.62;]")
        testAgent("Mozilla/5.0 (Linux; Android 7.0; SM-G955F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/62.0.3202.84 Mobile Safari/537.36 [FB_IAB/Orca-Android;FBAV/142.0.0.18.63;]")
      }

    }
  }
}

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
