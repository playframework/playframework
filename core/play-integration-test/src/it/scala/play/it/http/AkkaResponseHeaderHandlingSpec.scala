/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.PlaySpecification
import play.api.test.Port
import play.it.AkkaHttpIntegrationSpecification
import play.it.LogTester

class AkkaResponseHeaderHandlingSpec extends PlaySpecification with AkkaHttpIntegrationSpecification {
  "support invalid http response headers and raise a warning" should {
    def withServer[T](action: (DefaultActionBuilder, PlayBodyParsers) => EssentialAction)(block: Port => T) = {
      runningWithPort(
        TestServer(
          testServerPort,
          GuiceApplicationBuilder()
            .appRoutes { app =>
              val Action = app.injector.instanceOf[DefaultActionBuilder]
              val parse  = app.injector.instanceOf[PlayBodyParsers]
              ({ case _ => action(Action, parse) })
            }
            .build()
        )
      ) { port =>
        block(port)
      }
    }

    "correct support invalid Authorization header" in withServer((Action, _) =>
      Action { rh =>
        // authorization is a invalid response header
        Results.Ok.withHeaders("Authorization" -> "invalid")
      }
    ) { port =>
      val responses = BasicHttpClient.makeRequests(port, trickleFeed = Some(100L))(
        // Second request ensures that Play switches back to its normal handler
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses.length must_== 1
      responses(0).status must_== 200
      responses(0).headers.get("Authorization") must beSome("invalid")
    }

    "don't strip quotes from Link header" in withServer((Action, _) =>
      Action { rh =>
        // Test the header reported in https://github.com/playframework/playframework/issues/7733
        Results.Ok.withHeaders("Link" -> """<http://example.com/some/url>; rel="next"""")
      }
    ) { port =>
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses(0).headers.get("Link") must beSome("""<http://example.com/some/url>; rel="next"""")
    }

    "don't log a warning for Set-Cookie headers with negative ages" in {
      val problemHeaderValue = "PLAY_FLASH=; Max-Age=-86400; Expires=Tue, 30 Jan 2018 06:29:53 GMT; Path=/; HTTPOnly"
      withServer((Action, _) =>
        Action { rh =>
          // Test the header reported in https://github.com/playframework/playframework/issues/8205
          Results.Ok.withHeaders("Set-Cookie" -> problemHeaderValue)
        }
      ) { port =>
        val (Seq(response), logMessages) = LogTester.recordLogEvents {
          BasicHttpClient.makeRequests(port)(
            BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
          )
        }
        response.status must_== 200
        logMessages.map(_.getFormattedMessage) must not contain (contain(problemHeaderValue))
      }
    }
  }
}
