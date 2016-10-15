/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import java.util.zip.Deflater

import akka.stream.scaladsl.{ Flow, Sink }
import akka.util.ByteString
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.test._
import play.it._

import scala.concurrent.ExecutionContext.Implicits._
import scala.util.Random

class NettyRequestBodyHandlingSpec extends RequestBodyHandlingSpec with NettyIntegrationSpecification
class AkkaHttpRequestBodyHandlingSpec extends RequestBodyHandlingSpec with AkkaHttpIntegrationSpecification

trait RequestBodyHandlingSpec extends PlaySpecification with ServerIntegrationSpecification {

  sequential

  "Play request body handling" should {

    def withServer[T](action: (DefaultActionBuilder, PlayBodyParsers) => EssentialAction)(block: Port => T) = {
      val port = testServerPort
      running(TestServer(port, GuiceApplicationBuilder().appRoutes { app =>
        val Action = app.injector.instanceOf[DefaultActionBuilder]
        val parse = app.injector.instanceOf[PlayBodyParsers]
        ({ case _ => action(Action, parse) })
      }.build())) {
        block(port)
      }
    }

    "handle gzip bodies" in withServer((Action, _) => Action { rh =>
      Results.Ok(rh.body.asText.getOrElse(""))
    }) { port =>
      val bodyString = "Hello World"

      // Compress the bytes
      var output = new Array[Byte](100)
      val compressor = new Deflater()
      compressor.setInput(bodyString.getBytes("UTF-8"))
      compressor.finish()
      val compressedDataLength = compressor.deflate(output)

      val client = new BasicHttpClient(port, false)
      val response = client.sendRaw(output, Map("Content-Type" -> "text/plain", "Content-Length" -> compressedDataLength.toString, "Content-Encoding" -> "deflate"))
      response.status must_== 200
      response.body.left.get must_== bodyString
    }

    "handle large bodies" in withServer((_, _) => EssentialAction { rh =>
      Accumulator(Sink.ignore).map(_ => Results.Ok)
    }) { port =>
      val body = new String(Random.alphanumeric.take(50 * 1024).toArray)
      val responses = BasicHttpClient.makeRequests(port, trickleFeed = Some(100L))(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body),
        // Second request ensures that Play switches back to its normal handler
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses.length must_== 2
      responses(0).status must_== 200
      responses(1).status must_== 200
    }

    "gracefully handle early body parser termination" in withServer((_, _) => EssentialAction { rh =>
      Accumulator(Sink.ignore).through(Flow[ByteString].take(10)).map(_ => Results.Ok)
    }) { port =>
      val body = new String(Random.alphanumeric.take(50 * 1024).toArray)
      // Trickle feed is important, otherwise it won't switch to ignoring the body.
      val responses = BasicHttpClient.makeRequests(port, trickleFeed = Some(100L))(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body),
        // Second request ensures that Play switches back to its normal handler
        BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
      )
      responses.length must_== 2
      responses(0).status must_== 200
      responses(1).status must_== 200
    }
  }
}
