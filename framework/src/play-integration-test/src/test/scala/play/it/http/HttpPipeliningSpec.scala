/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import akka.stream.scaladsl.Source
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.streams.Accumulator
import play.api.mvc.{ Results, EssentialAction }
import play.api.test._
import play.it._
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import akka.pattern.after

import scala.concurrent.ExecutionContext.Implicits.global

object NettyHttpPipeliningSpec extends HttpPipeliningSpec with NettyIntegrationSpecification
object AkkaHttpHttpPipeliningSpec extends HttpPipeliningSpec with AkkaHttpIntegrationSpecification

trait HttpPipeliningSpec extends PlaySpecification with ServerIntegrationSpecification {

  val actorSystem = akka.actor.ActorSystem()

  "Play's http pipelining support" should {

    def withServer[T](action: EssentialAction)(block: Port => T) = {
      val port = testServerPort
      running(TestServer(port, GuiceApplicationBuilder().routes { case _ => action }.build())) {
        block(port)
      }
    }

    "wait for the first response to return before returning the second" in withServer(EssentialAction { req =>
      req.path match {
        case "/long" => Accumulator.done(after(100.milliseconds, actorSystem.scheduler)(Future(Results.Ok("long"))))
        case "/short" => Accumulator.done(Results.Ok("short"))
        case _ => Accumulator.done(Results.NotFound)
      }
    }) { port =>
      val responses = BasicHttpClient.pipelineRequests(port,
        BasicRequest("GET", "/long", "HTTP/1.1", Map(), ""),
        BasicRequest("GET", "/short", "HTTP/1.1", Map(), "")
      )
      responses(0).status must_== 200
      responses(0).body must beLeft("long")
      responses(1).status must_== 200
      responses(1).body must beLeft("short")
    }

    "wait for the first response body to return before returning the second" in withServer(EssentialAction { req =>
      req.path match {
        case "/long" => Accumulator.done(
          Results.Ok.chunked(Source.tick(initialDelay = 50.milliseconds, interval = 50.milliseconds, tick = "chunk").take(3))
        )
        case "/short" => Accumulator.done(Results.Ok("short"))
        case _ => Accumulator.done(Results.NotFound)
      }
    }) { port =>
      val responses = BasicHttpClient.pipelineRequests(port,
        BasicRequest("GET", "/long", "HTTP/1.1", Map(), ""),
        BasicRequest("GET", "/short", "HTTP/1.1", Map(), "")
      )
      responses(0).status must_== 200
      responses(0).body must beRight
      responses(0).body.right.get._1 must containAllOf(Seq("chunk", "chunk", "chunk")).inOrder
      responses(1).status must_== 200
      responses(1).body must beLeft("short")
    }

  }
}
