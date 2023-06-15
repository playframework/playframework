/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.io.IOException
import java.util.Properties

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.jdk.CollectionConverters._
import scala.util.Random

import org.apache.pekko.stream.scaladsl.Sink
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.streams.Accumulator
import play.api.mvc.EssentialAction
import play.api.mvc.Results
import play.api.test._
import play.api.Mode
import play.core.server._
import play.it.PekkoHttpIntegrationSpecification

class PekkoRequestTimeoutSpec extends PlaySpecification with PekkoHttpIntegrationSpecification {
  "play.server.akka.requestTimeout configuration" should {
    def withServer[T](httpTimeout: Duration)(action: EssentialAction)(block: Port => T) = {
      def getTimeout(d: Duration) = d match {
        case Duration.Inf   => "null"
        case Duration(t, u) => s"${u.toMillis(t)}ms"
      }
      val props = new Properties(System.getProperties)
      (props: java.util.Map[Object, Object]).putAll(
        Map(
          "play.server.akka.requestTimeout" -> getTimeout(httpTimeout)
        ).asJava
      )
      val serverConfig = ServerConfig(port = Some(testServerPort), mode = Mode.Test, properties = props)
      runningWithPort(
        play.api.test.TestServer(
          config = serverConfig,
          application = new GuiceApplicationBuilder()
            .routes {
              case _ => action
            }
            .build(),
          serverProvider = Some(integrationServerProvider)
        )
      ) { port =>
        block(port)
      }
    }

    def doRequests(port: Int) = {
      val body = new String(Random.alphanumeric.take(50 * 1024).toArray)
      val responses = BasicHttpClient.makeRequests(port)(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body),
        // Second request ensures that Play switches back to its normal handler
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses
    }

    "support sub-second timeouts" in withServer(300.millis)(EssentialAction { req =>
      Accumulator(Sink.ignore).map { _ =>
        Thread.sleep(1400L)
        Results.Ok
      }
    }) { port => doRequests(port) must throwA[IOException] }

    "support multi-second timeouts" in withServer(1500.millis)(EssentialAction { req =>
      Accumulator(Sink.ignore).map { _ =>
        Thread.sleep(1600L)
        Results.Ok
      }
    }) { port => doRequests(port) must throwA[IOException] }

    "not timeout for slow requests with a sub-second timeout" in withServer(700.millis)(EssentialAction { req =>
      Accumulator(Sink.ignore).map { _ =>
        Thread.sleep(400L)
        Results.Ok
      }
    }) { port =>
      val responses = doRequests(port)
      responses.length must_== 2
      responses(0).status must_== 200
      responses(1).status must_== 200
    }

    "not timeout for slow requests with a multi-second timeout" in withServer(1500.millis)(EssentialAction { req =>
      Accumulator(Sink.ignore).map { _ =>
        Thread.sleep(1000L)
        Results.Ok
      }
    }) { port =>
      val responses = doRequests(port)
      responses.length must_== 2
      responses(0).status must_== 200
      responses(1).status must_== 200
    }
  }
}
