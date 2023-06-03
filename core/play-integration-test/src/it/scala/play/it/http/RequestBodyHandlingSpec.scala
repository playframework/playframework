/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.util.zip.Deflater

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Random

import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.http.HttpErrorHandler
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.test._
import play.api.Configuration
import play.api.Mode
import play.core.server.ServerConfig
import play.it._

class NettyRequestBodyHandlingSpec    extends RequestBodyHandlingSpec with NettyIntegrationSpecification
class AkkaHttpRequestBodyHandlingSpec extends RequestBodyHandlingSpec with AkkaHttpIntegrationSpecification

trait RequestBodyHandlingSpec extends PlaySpecification with ServerIntegrationSpecification {

  "Play request body handling" should {
    def withServerAndConfig[T](
        configuration: (String, Any)*
    )(action: (DefaultActionBuilder, PlayBodyParsers) => EssentialAction)(block: Port => T) = {
      val config = Configuration(configuration: _*)
      val serverConfig: ServerConfig = {
        val c = ServerConfig(port = Some(testServerPort), mode = Mode.Test)
        c.copy(configuration = config.withFallback(c.configuration))
      }
      runningWithPort(
        play.api.test.TestServer(
          serverConfig,
          GuiceApplicationBuilder()
            .appRoutes { app =>
              val Action = app.injector.instanceOf[DefaultActionBuilder]
              val parse  = app.injector.instanceOf[PlayBodyParsers]
              ({ case _ => action(Action, parse) })
            }
            .configure(config)
            .build(),
          Some(integrationServerProvider)
        )
      ) { port =>
        block(port)
      }
    }

    def withServer[T](action: (DefaultActionBuilder, PlayBodyParsers) => EssentialAction)(block: Port => T) = {
      withServerAndConfig()(action)(block)
    }

    "handle gzip bodies" in withServer((Action, _) => Action { rh => Results.Ok(rh.body.asText.getOrElse("")) }) {
      port =>
        val bodyString = "Hello World"

        // Compress the bytes
        val output     = new Array[Byte](100)
        val compressor = new Deflater()
        compressor.setInput(bodyString.getBytes("UTF-8"))
        compressor.finish()
        val compressedDataLength = compressor.deflate(output)

        val client = new BasicHttpClient(port, false)
        val response = client.sendRaw(
          output.take(compressedDataLength),
          Map(
            "Content-Type"     -> "text/plain",
            "Content-Length"   -> compressedDataLength.toString,
            "Content-Encoding" -> "deflate"
          )
        )
        response.status must_== 200
        response.body.left.get must_== bodyString
    }

    "handle large bodies" in withServer((_, _) =>
      EssentialAction { rh => Accumulator(Sink.ignore).map(_ => Results.Ok) }
    ) { port =>
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

    "gracefully handle early body parser termination" in withServer((_, _) =>
      EssentialAction { rh => Accumulator(Sink.ignore).through(Flow[ByteString].take(10)).map(_ => Results.Ok) }
    ) { port =>
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

    "handle a big http request" in withServer((Action, parse) =>
      Action(parse.default(Some(Long.MaxValue))) { rh => Results.Ok(rh.body.asText.getOrElse("")) }
    ) { port =>
      // big body that should not crash akka and netty
      val body = "Hello World" * (1024 * 1024)
      val responses = BasicHttpClient.makeRequests(port, trickleFeed = Some(1))(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body)
      )
      responses.length must_== 1
      responses(0).status must_== 200
    }

    "handle a big http request and fail with HTTP Error '413 request entity too large'" in withServerAndConfig(
      "play.server.max-content-length" -> "21b",
      "play.http.errorHandler"         -> classOf[CustomErrorHandler].getName,
    )((Action, parse) =>
      Action(parse.default(Some(Long.MaxValue))) { rh => Results.Ok(rh.body.asText.getOrElse("")) }
    ) { port =>
      val body = "Hello World" * 2 // => 22 bytes, but we allow only 21 bytes
      val responses = BasicHttpClient.makeRequests(port, trickleFeed = Some(100L))(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body)
      )
      responses.length must_== 1
      responses(0).status must_== 413
      responses(0).body.left.getOrElse("") must_=== "Origin: server-backend / Request Entity Too Large"
    }

    "handle a big http request with exact amount of allowed Content-Length" in withServerAndConfig(
      "play.server.max-content-length" -> "22b"
    )((Action, parse) =>
      Action(parse.default(Some(Long.MaxValue))) { rh => Results.Ok(rh.body.asText.getOrElse("")) }
    ) { port =>
      val body = "Hello World" * 2 // => 22 bytes, same what we allow
      val responses = BasicHttpClient.makeRequests(port, trickleFeed = Some(100L))(
        BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body)
      )
      responses.length must_== 1
      responses(0).status must_== 200
    }
  }
}

class CustomErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String) =
    Future.successful(
      Results.Status(statusCode)(
        "Origin: " + request.attrs
          .get(HttpErrorHandler.Attrs.HttpErrorInfo)
          .map(_.origin)
          .getOrElse("<not set>") + " / " + message
      )
    )
  def onServerError(request: RequestHeader, exception: Throwable) =
    Future.successful(Results.BadRequest)
}
