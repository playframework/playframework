/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.websocket

import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.Inflater

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.reflect.ClassTag

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.Props
import org.apache.pekko.actor.Status
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.util.ByteString
import org.apache.pekko.util.Timeout
import org.specs2.execute.AsResult
import org.specs2.execute.EventuallyResults
import org.specs2.matcher.Matcher
import org.specs2.specification.AroundEach
import play.api.http.websocket._
import play.api.http.HttpErrorHandler
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc.Cookie
import play.api.mvc.CookieHeaderEncoding
import play.api.mvc.DiscardingCookie
import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.mvc.SessionCookieBaker
import play.api.mvc.WebSocket
import play.api.routing.HandlerDef
import play.api.test._
import play.api.Application
import play.api.Configuration
import play.it._
import play.it.http.websocket.WebSocketClient.CompressionMode
import play.it.http.websocket.WebSocketClient.ContinuationMessage
import play.it.http.websocket.WebSocketClient.ExtendedMessage
import play.it.http.websocket.WebSocketClient.RawTextMessage
import play.it.http.websocket.WebSocketClient.RawWebSocketFrame
import play.it.http.websocket.WebSocketClient.SimpleMessage

trait WebSocketCompressionSpec extends WebSocketSpecMethods {
  def backendName: String
  def extraConfiguredCompressionConfig: Map[String, Any] = Map.empty
  def expectedServerMaxWindowBits: Option[Int]           = None

  "Plays WebSockets using " + backendName + " with compression" should {
    "negotiate permessage-deflate" in {
      withServer(app =>
        WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) }
      ) { (app, port) =>
        import app.materializer
        val (_, headers) = runWebSocket(
          port,
          { flow =>
            Source.empty[ExtendedMessage].via(flow).runWith(Sink.ignore)
            Future.successful(())
          },
          subprotocol = None,
          handleConnect = c => c,
          compressionMode = CompressionMode.Enabled()
        )

        headers.collectFirst {
          case (name, value) if name.equalsIgnoreCase("Sec-WebSocket-Extensions") => value
        } must beSome(contain("permessage-deflate"))
      }
    }

    "decompress inbound permessage-deflate frames before passing them to the application" in {
      val consumed = Promise[List[String]]()
      withServer(app =>
        WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(onFramesConsumed[String](consumed.success(_)), Source.maybe[String])
        }
      ) { (app, port) =>
        import app.materializer
        val result = runWebSocket(
          port,
          { flow =>
            sendFrames(
              TextMessage("compressed client message"),
              CloseMessage(1000)
            ).via(flow).runWith(Sink.ignore)
            consumed.future
          },
          compressionMode = CompressionMode.Enabled()
        )

        result must_== Seq("compressed client message")
      }
    }

    "compress outbound permessage-deflate frames sent by the application" in {
      withServer(app =>
        WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(Sink.ignore, Source.single("compressed server message"))
        }
      ) { (app, port) =>
        import app.materializer
        val frames = runWebSocket(
          port,
          { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
          compressionMode = CompressionMode.RequestOnly()
        )

        frames.collectFirst {
          case RawWebSocketFrame("text", data, rsv, true) => (data, rsv)
        } must beSome[(ByteString, Int)].which {
          case (data, rsv) =>
            data.nonEmpty &&
            (rsv & 4) == 4 &&
            data != ByteString("compressed server message") &&
            inflatePerMessageDeflate(data).utf8String == "compressed server message"
        }
      }
    }

    "compress only outbound messages above the configured threshold" in {
      val exactText        = "12345"
      val utf8Text         = "\u20ac\u20ac"
      val exactBinary      = ByteString(1, 2, 3, 4, 5)
      val overBinary       = ByteString(1, 2, 3, 4, 5, 6)
      val outboundMessages = List[Message](
        TextMessage(exactText),
        TextMessage(utf8Text),
        BinaryMessage(exactBinary),
        BinaryMessage(overBinary)
      )

      withServer(
        app =>
          WebSocket.accept[Message, Message] { req =>
            Flow.fromSinkAndSource(Sink.ignore, Source(outboundMessages))
          },
        Map(
          "play.server.websocket.compression.threshold"                              -> "5 bytes",
          "play.server.websocket.compression.perMessageDeflate.allowServerNoContext" -> true
        )
      ) { (app, port) =>
        import app.materializer
        val frames = runWebSocket(
          port,
          { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
          compressionMode = CompressionMode.RequestOnly("permessage-deflate; server_no_context_takeover")
        )

        val dataFrames = frames.collect {
          case frame @ SimpleMessage(_: TextMessage, _)     => frame
          case frame @ SimpleMessage(_: BinaryMessage, _)   => frame
          case frame @ RawWebSocketFrame("text", _, _, _)   => frame
          case frame @ RawWebSocketFrame("binary", _, _, _) => frame
        }

        dataFrames must beLike {
          case List(
                SimpleMessage(TextMessage(`exactText`), true),
                RawWebSocketFrame("text", compressedText, textRsv, true),
                SimpleMessage(BinaryMessage(`exactBinary`), true),
                RawWebSocketFrame("binary", compressedBinary, binaryRsv, true)
              ) =>
            (textRsv must_== 4)
              .and(inflatePerMessageDeflate(compressedText).utf8String must_== utf8Text)
              .and(binaryRsv must_== 4)
              .and(inflatePerMessageDeflate(compressedBinary) must_== overBinary)
        }
      }
    }

    "allow the application to override the compression threshold for each outbound message" in {
      val belowText            = "\u20ac"
      val aboveText            = "123456"
      val belowBinary          = ByteString(1, 2, 3)
      val aboveBinary          = ByteString(1, 2, 3, 4, 5, 6)
      val compressibleMessages = List[Message](
        TextMessage(belowText),
        TextMessage(aboveText),
        BinaryMessage(belowBinary),
        BinaryMessage(aboveBinary)
      )
      val outboundMessages = compressibleMessages.patch(2, Seq(PingMessage(ByteString("ping"))), 0)
      val observedContexts = new AtomicReference(Vector.empty[(Message, Long, Boolean)])

      withServer(
        app =>
          WebSocket.acceptWithOptions[Message, Message] { req =>
            WebSocket.Accepted(
              Flow.fromSinkAndSource(Sink.ignore, Source(outboundMessages)),
              shouldCompress = context => {
                observedContexts.synchronized {
                  observedContexts.set(
                    observedContexts.get() :+ (
                      context.message,
                      context.payloadLength,
                      context.isAboveCompressionThreshold
                    )
                  )
                }
                context.payloadLength == 3
              }
            )
          },
        Map(
          "play.server.websocket.compression.threshold"                              -> "5 bytes",
          "play.server.websocket.compression.perMessageDeflate.allowServerNoContext" -> true
        )
      ) { (app, port) =>
        import app.materializer
        val frames = runWebSocket(
          port,
          { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
          compressionMode = CompressionMode.RequestOnly("permessage-deflate; server_no_context_takeover")
        )

        val dataFrames = frames.collect {
          case frame @ SimpleMessage(_: TextMessage, _)     => frame
          case frame @ SimpleMessage(_: BinaryMessage, _)   => frame
          case frame @ RawWebSocketFrame("text", _, _, _)   => frame
          case frame @ RawWebSocketFrame("binary", _, _, _) => frame
        }

        val framesResult = dataFrames must beLike {
          case List(
                RawWebSocketFrame("text", compressedText, textRsv, true),
                SimpleMessage(TextMessage(`aboveText`), true),
                RawWebSocketFrame("binary", compressedBinary, binaryRsv, true),
                SimpleMessage(BinaryMessage(`aboveBinary`), true)
              ) =>
            (textRsv must_== 4)
              .and(inflatePerMessageDeflate(compressedText).utf8String must_== belowText)
              .and(binaryRsv must_== 4)
              .and(inflatePerMessageDeflate(compressedBinary) must_== belowBinary)
        }
        val contextsResult = observedContexts.get() must_== compressibleMessages.map { message =>
          val length = message match {
            case TextMessage(data)   => ByteString(data).length.toLong
            case BinaryMessage(data) => data.length.toLong
            case _                   => 0L
          }
          (message, length, length > 5)
        }

        framesResult.and(contextsResult).and(frames must contain(pingFrame(be_==("ping"))))
      }
    }

    "not evaluate the message compression selector when compression was not negotiated" in {
      val evaluated = new AtomicReference(false)
      withServer(app =>
        WebSocket.acceptWithOptions[String, String] { req =>
          WebSocket.Accepted(
            Flow.fromSinkAndSource(Sink.ignore, Source.single("plain server message")),
            shouldCompress = _ => {
              evaluated.set(true)
              true
            }
          )
        }
      ) { (app, port) =>
        import app.materializer
        val frames = runWebSocket(
          port,
          { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
          compressionMode = CompressionMode.Disabled
        )

        (frames.collectFirst {
          case SimpleMessage(TextMessage(data), true) => data
        } must beSome("plain server message")).and(evaluated.get() must beFalse)
      }
    }

    "allow Java applications to select outbound messages for compression" in {
      val javaWebSocket = WebSocketSpecJavaActions.selectMessagesForCompression()
      val javaHandler   = play.core.routing.HandlerInvokerFactory.javaWebSocket
        .createInvoker(
          javaWebSocket,
          HandlerDef(
            javaWebSocket.getClass.getClassLoader,
            "package",
            "controller",
            "method",
            Nil,
            "GET",
            "/stream"
          )
        )
        .call(javaWebSocket)
      withServer(
        _ => javaHandler,
        Map(
          "play.server.websocket.compression.threshold"                              -> "5 bytes",
          "play.server.websocket.compression.perMessageDeflate.allowServerNoContext" -> true
        )
      ) { (app, port) =>
        import app.materializer
        val frames = runWebSocket(
          port,
          { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
          compressionMode = CompressionMode.RequestOnly("permessage-deflate; server_no_context_takeover")
        )

        val dataFrames = frames.collect {
          case frame @ SimpleMessage(_: TextMessage, _)   => frame
          case frame @ RawWebSocketFrame("text", _, _, _) => frame
        }
        dataFrames must beLike {
          case List(
                RawWebSocketFrame("text", compressedText, rsv, true),
                SimpleMessage(TextMessage("123456"), true)
              ) =>
            (rsv must_== 4).and(inflatePerMessageDeflate(compressedText).utf8String must_== "\u20ac")
        }
      }
    }

    "not negotiate compression when websocket compression is disabled" in {
      withServer(
        app =>
          WebSocket.acceptWithOptions[String, String] { req =>
            WebSocket.Accepted(
              Flow.fromSinkAndSource(Sink.ignore, Source.single("plain server message")),
              shouldCompress = _ => throw new AssertionError("compression selector must not be evaluated")
            )
          },
        Map("play.server.websocket.compression.enabled" -> false)
      ) { (app, port) =>
        import app.materializer
        val (frames, headers) = runWebSocket(
          port,
          { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
          subprotocol = None,
          handleConnect = c => c,
          compressionMode = CompressionMode.RequestOnly()
        )

        headers.collectFirst {
          case (name, value) if name.equalsIgnoreCase("Sec-WebSocket-Extensions") => value
        } must beNone

        frames.collectFirst {
          case SimpleMessage(TextMessage(data), true) => data
        } must beSome("plain server message")
      }
    }

    "not negotiate compression when compression is disabled for the accepted WebSocket" in {
      withServer(app =>
        WebSocket.acceptWithOptions[String, String] { req =>
          WebSocket.Accepted(
            Flow.fromSinkAndSource(Sink.ignore, Source.single("plain server message")),
            compressionEnabled = false,
            shouldCompress = _ => throw new AssertionError("compression selector must not be evaluated")
          )
        }
      ) { (app, port) =>
        import app.materializer
        val (frames, headers) = runWebSocket(
          port,
          { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
          subprotocol = None,
          handleConnect = c => c,
          compressionMode = CompressionMode.RequestOnly()
        )

        headers.collectFirst {
          case (name, value) if name.equalsIgnoreCase("Sec-WebSocket-Extensions") => value
        } must beNone

        frames.collectFirst {
          case SimpleMessage(TextMessage(data), true) => data
        } must beSome("plain server message")
      }
    }

    "use configured permessage-deflate negotiation settings" in {
      withServer(
        app => WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) },
        Map(
          "play.server.websocket.compression.perMessageDeflate.preferredClientWindowSize" -> 11,
          "play.server.websocket.compression.perMessageDeflate.allowServerNoContext"      -> true,
          "play.server.websocket.compression.perMessageDeflate.preferredClientNoContext"  -> true,
          "play.server.websocket.compression.perMessageDeflate.compressionLevel"          -> 5
        ) ++ extraConfiguredCompressionConfig
      ) { (app, port) =>
        import app.materializer
        val (_, headers) = runWebSocket(
          port,
          { flow =>
            Source.empty[ExtendedMessage].via(flow).runWith(Sink.ignore)
            Future.successful(())
          },
          subprotocol = None,
          handleConnect = c => c,
          compressionMode = CompressionMode.RequestOnly(
            "permessage-deflate; client_max_window_bits; server_max_window_bits=15; " +
              "client_no_context_takeover; server_no_context_takeover"
          )
        )

        val extension = headers.collectFirst {
          case (name, value) if name.equalsIgnoreCase("Sec-WebSocket-Extensions") => value
        }

        extension must beSome[String].which { value =>
          value.contains("permessage-deflate") &&
          expectedServerMaxWindowBits.forall(bits => value.contains(s"server_max_window_bits=$bits")) &&
          value.contains("client_max_window_bits=11") &&
          value.contains("server_no_context_takeover") &&
          value.contains("client_no_context_takeover")
        }
      }
    }
  }
}
class NettyWebSocketSpec extends WebSocketSpec with NettyIntegrationSpecification with WebSocketCompressionSpec {
  override def backendName: String                                = "netty backend"
  override def extraConfiguredCompressionConfig: Map[String, Any] =
    Map(
      "play.server.netty.websocket.compression.perMessageDeflate.allowServerWindowSize" -> true,
      "play.server.netty.websocket.compression.perMessageDeflate.serverWindowSize"      -> 12,
      "play.server.netty.websocket.compression.perMessageDeflate.memLevel"              -> 7
    )
  override def expectedServerMaxWindowBits: Option[Int] = Some(12)

  "Plays WebSockets using netty backend with compression" should {
    def failToStartWithInvalidCompressionSetting(path: String, value: Any) = {
      withServer(
        app => WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) },
        Map(path -> value)
      ) { (_, _) =>
        ()
      } must throwA[RuntimeException].like {
        case exception =>
          (exception.getCause must beAnInstanceOf[play.core.server.ServerStartException])
            .and(exception.getMessage must contain(path))
      }
    }

    "fail at startup when compressionLevel is below its valid range" in {
      failToStartWithInvalidCompressionSetting(
        "play.server.websocket.compression.perMessageDeflate.compressionLevel",
        -1
      )
    }

    "fail at startup when compressionLevel is above its valid range" in {
      failToStartWithInvalidCompressionSetting(
        "play.server.websocket.compression.perMessageDeflate.compressionLevel",
        10
      )
    }

    "fail at startup when preferredClientWindowSize is below its valid range" in {
      failToStartWithInvalidCompressionSetting(
        "play.server.websocket.compression.perMessageDeflate.preferredClientWindowSize",
        7
      )
    }

    "fail at startup when preferredClientWindowSize is above its valid range" in {
      failToStartWithInvalidCompressionSetting(
        "play.server.websocket.compression.perMessageDeflate.preferredClientWindowSize",
        16
      )
    }

    "fail at startup when serverWindowSize is below its valid range" in {
      failToStartWithInvalidCompressionSetting(
        "play.server.netty.websocket.compression.perMessageDeflate.serverWindowSize",
        7
      )
    }

    "fail at startup when serverWindowSize is above its valid range" in {
      failToStartWithInvalidCompressionSetting(
        "play.server.netty.websocket.compression.perMessageDeflate.serverWindowSize",
        16
      )
    }

    "fail at startup when memLevel is below its valid range" in {
      failToStartWithInvalidCompressionSetting(
        "play.server.netty.websocket.compression.perMessageDeflate.memLevel",
        0
      )
    }

    "fail at startup when memLevel is above its valid range" in {
      failToStartWithInvalidCompressionSetting(
        "play.server.netty.websocket.compression.perMessageDeflate.memLevel",
        10
      )
    }

    "fail at startup when allowServerWindowSize is invalid" in {
      failToStartWithInvalidCompressionSetting(
        "play.server.netty.websocket.compression.perMessageDeflate.allowServerWindowSize",
        "invalid"
      )
    }

    "fail clearly when Netty websocket compression maxAllocation exceeds Netty's integer limit" in {
      withServer(
        app => WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) },
        Map("play.server.websocket.compression.maxAllocation" -> "3g")
      ) { (_, _) =>
        ()
      } must throwA[RuntimeException].like {
        case exception =>
          (exception.getCause must beAnInstanceOf[play.core.server.ServerStartException])
            .and(exception.getMessage must contain("play.server.websocket.compression.maxAllocation"))
      }
    }
  }
}

class PekkoHttpWebSocketSpec
    extends WebSocketSpec
    with PekkoHttpIntegrationSpecification
    with WebSocketCompressionSpec {
  override def backendName: String = "pekko-http backend"

  "Plays WebSockets using pekko-http backend with HTTP2 enabled" should {
    "time out after play.server.http.idleTimeout" in delayedSend(
      delay = 5.seconds, // connection times out before something gets send
      idleTimeout = "3 seconds",
      expectedMessages = Seq(),
      pekkoHttp2enabled = true,
    )

    "not time out within play.server.http.idleTimeout" in delayedSend(
      delay = 3.seconds, // something gets send before connection times out
      idleTimeout = "5 seconds",
      expectedMessages = Seq("foo"),
      pekkoHttp2enabled = true,
    )
  }
}

class NettyPingWebSocketOnlySpec     extends PingWebSocketSpec with NettyIntegrationSpecification
class PekkoHttpPingWebSocketOnlySpec extends PingWebSocketSpec with PekkoHttpIntegrationSpecification

trait PingWebSocketSpec
    extends PlaySpecification
    with WsTestClient
    with ServerIntegrationSpecification
    with WebSocketSpecMethods {

  "backend server" should {
    "respond to pings" in {
      withServer(app =>
        WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) }
      ) { (app, port) =>
        import app.materializer
        val frames = runWebSocket(
          port,
          { flow =>
            sendFrames(
              PingMessage(ByteString("hello")),
              CloseMessage(1000)
            ).via(flow).runWith(consumeFrames)
          }
        )
        frames must contain(
          exactly(
            pongFrame(be_==("hello")),
            closeFrame()
          )
        )
      }
    }

    "not respond to pongs" in {
      withServer(app =>
        WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) }
      ) { (app, port) =>
        import app.materializer
        val frames = runWebSocket(
          port,
          { flow =>
            sendFrames(
              PongMessage(ByteString("hello")),
              CloseMessage(1000)
            ).via(flow).runWith(consumeFrames)
          }
        )
        frames must contain(
          exactly(
            closeFrame()
          )
        )
      }
    }

    "respond to pings before the application requests a message" in {
      val appMessages = Promise[SinkQueueWithCancel[Message]]()

      withServer(app =>
        WebSocket.accept[Message, Message] { req =>
          Flow
            .fromSinkAndSourceMat(Sink.queue[Message](), Source.maybe[Message])(Keep.left)
            .mapMaterializedValue { queue =>
              appMessages.success(queue)
              queue
            }
        }
      ) { (app, port) =>
        import app.materializer
        val (pong, appPing, close) = runWebSocket(
          port,
          { flow =>
            val (clientIn, clientOut) =
              Source
                .queue[ExtendedMessage](1, OverflowStrategy.backpressure)
                .via(flow)
                .toMat(Sink.queue[ExtendedMessage]())(Keep.both)
                .run()

            for {
              appQueue <- appMessages.future
              _        <- clientIn.offer(PingMessage(ByteString("hello")))
              pong     <- clientOut.pull()
              appPing  <- appQueue.pull()
              appClose = appQueue.pull()
              _     <- clientIn.offer(CloseMessage(CloseCodes.Regular))
              close <- clientOut.pull()
              _     <- appClose
              _     <- clientOut.pull()
            } yield (pong, appPing, close)
          }
        )

        (pong must beSome(pongFrame(be_==("hello"))))
          .and(appPing must beSome(beEqualTo(PingMessage(ByteString("hello")))))
          .and(close must beSome(closeFrame()))
      }
    }

    "acknowledge a close before the application requests a message" in {
      val appMessages = Promise[SinkQueueWithCancel[Message]]()

      withServer(app =>
        WebSocket.accept[Message, Message] { req =>
          Flow
            .fromSinkAndSourceMat(Sink.queue[Message](), Source.maybe[Message])(Keep.left)
            .mapMaterializedValue { queue =>
              appMessages.success(queue)
              queue
            }
        }
      ) { (app, port) =>
        import app.materializer
        val (remoteClose, appClose) = runWebSocket(
          port,
          { flow =>
            val (clientIn, clientOut) =
              Source
                .queue[ExtendedMessage](1, OverflowStrategy.backpressure)
                .via(flow)
                .toMat(Sink.queue[ExtendedMessage]())(Keep.both)
                .run()

            for {
              appQueue    <- appMessages.future
              _           <- clientIn.offer(CloseMessage(CloseCodes.GoingAway))
              remoteClose <- clientOut.pull()
              appClose    <- appQueue.pull()
              _           <- clientOut.pull()
              _           <- appQueue.pull()
            } yield (remoteClose, appClose)
          }
        )

        (remoteClose must beSome(closeFrame(CloseCodes.GoingAway)))
          .and(appClose must beSome(closeMessage(CloseCodes.GoingAway)))
      }
    }

    "ping client every 2 seconds, 4 times total within 9 seconds" in handleKeepAlive(
      "ping",
      "2 seconds",
      9.seconds,
      List.fill(4)(pingFrame(be_==("")))
    )
    "ping client every 3 seconds, 2 times total within 8 seconds" in handleKeepAlive(
      "ping",
      "3 seconds",
      8.seconds,
      List.fill(2)(pingFrame(be_==("")))
    )
    "never ping client 9 seconds" in handleKeepAlive("ping", "infinite", 9.seconds, List.empty)
    "pong client every 2 seconds, 4 times total within 9 seconds" in handleKeepAlive(
      "pong",
      "2 seconds",
      9.seconds,
      List.fill(4)(pongFrame(be_==("")))
    )
    "pong client every 3 seconds, 2 times total within 8 seconds" in handleKeepAlive(
      "pong",
      "3 seconds",
      8.seconds,
      List.fill(2)(pongFrame(be_==("")))
    )
    "never pong client 9 seconds" in handleKeepAlive("pong", "infinite", 9.seconds, List.empty)
  }
}

trait WebSocketSpec
    extends PlaySpecification
    with WsTestClient
    with ServerIntegrationSpecification
    with WebSocketSpecMethods
    with PingWebSocketSpec {
  case class JsonMessage(name: String)

  implicit val jsonMessageReads: Reads[JsonMessage]                                               = Json.reads[JsonMessage]
  implicit val jsonMessageFlowTransformer: WebSocket.MessageFlowTransformer[JsonMessage, JsValue] =
    WebSocket.MessageFlowTransformer.jsonMessageFlowTransformer[JsonMessage, JsValue]

  /*
   * This is the flakiest part of the test suite -- the CI server will timeout websockets
   * and fail tests seemingly at random.
   */
  override def aroundEventually[R: AsResult](r: => R) = {
    EventuallyResults.eventually[R](5, 100.milliseconds)(r)
  }

  "Plays WebSockets" should {
    "time out after play.server.http.idleTimeout" in delayedSend(
      delay = 5.seconds, // connection times out before something gets send
      idleTimeout = "3 seconds",
      expectedMessages = Seq()
    )

    "not time out within play.server.http.idleTimeout" in delayedSend(
      delay = 3.seconds, // something gets send before connection times out
      idleTimeout = "5 seconds",
      expectedMessages = Seq("foo")
    )

    "reject a zero play.server.websocket.closeTimeout at startup" in {
      failToStartWithInvalidCloseTimeout("0 seconds")
    }

    "reject a negative play.server.websocket.closeTimeout at startup" in {
      failToStartWithInvalidCloseTimeout("-1 second")
    }

    "reject an infinite play.server.websocket.closeTimeout at startup" in {
      failToStartWithInvalidCloseTimeout("infinite")
    }

    "allow handling WebSockets using Pekko streams" in {
      "allow consuming messages" in allowConsumingMessages { _ => consumed =>
        WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(onFramesConsumed[String](consumed.success(_)), Source.maybe[String])
        }
      }

      "allow sending messages" in allowSendingMessages { _ => messages =>
        WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source(messages)) }
      }

      "notify the application when the connection closes without a close frame" in {
        val consumed = Promise[List[Message]]()
        withServer(app =>
          WebSocket.accept[Message, Message] { req =>
            Flow.fromSinkAndSource(onFramesConsumed[Message](consumed.success(_)), Source.maybe[Message])
          }
        ) { (app, port) =>
          import app.materializer
          val result = runWebSocket(
            port,
            { flow =>
              Source.empty[ExtendedMessage].via(flow).runWith(Sink.ignore)
              consumed.future
            }
          )
          result must contain(exactly(closeMessage(1006)))
        }
      }

      "not notify the application with 1006 when the client sends a close frame" in {
        val consumed = Promise[List[Message]]()
        withServer(app =>
          WebSocket.accept[Message, Message] { req =>
            Flow.fromSinkAndSource(onFramesConsumed[Message](consumed.success(_)), Source.maybe[Message])
          }
        ) { (app, port) =>
          import app.materializer
          val result = runWebSocket(
            port,
            { flow =>
              sendFrames(CloseMessage(1000)).via(flow).runWith(Sink.ignore)
              consumed.future
            }
          )
          result must contain(exactly(closeMessage(1000)))
        }
      }

      "not expose 1006 as a typed WebSocket message" in {
        val consumed = Promise[List[String]]()
        withServer(app =>
          WebSocket.accept[String, String] { req =>
            Flow.fromSinkAndSource(onFramesConsumed[String](consumed.success(_)), Source.maybe[String])
          }
        ) { (app, port) =>
          import app.materializer
          val result = runWebSocket(
            port,
            { flow =>
              Source.empty[ExtendedMessage].via(flow).runWith(Sink.ignore)
              consumed.future
            }
          )
          result must beEmpty
        }
      }

      "not notify the application with 1006 when the application sends a close frame" in {
        val consumed = Promise[List[Message]]()
        withServer(app =>
          WebSocket.accept[Message, Message] { req =>
            Flow.fromSinkAndSource(onFramesConsumed[Message](consumed.success(_)), Source.single(CloseMessage(1000)))
          }
        ) { (app, port) =>
          import app.materializer
          val result = runWebSocket(
            port,
            { flow =>
              Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames)
              consumed.future
            }
          )
          result must beEmpty
        }
      }

      "terminate after closeTimeout when the client continues sending ping and pong frames" in {
        val consumed = Promise[List[Message]]()
        withServer(
          app =>
            WebSocket.accept[Message, Message] { req =>
              Flow.fromSinkAndSource(
                onFramesConsumed[Message](consumed.success(_)),
                Source.single(CloseMessage(CloseCodes.Regular))
              )
            },
          Map(
            "play.server.http.idleTimeout"                       -> "infinite",
            "play.server.https.idleTimeout"                      -> "infinite",
            "play.server.websocket.closeTimeout"                 -> "300 milliseconds",
            "play.server.websocket.periodic-keep-alive-max-idle" -> "infinite"
          )
        ) { (app, port) =>
          import app.materializer
          val closeReceived = Promise[Long]()
          val frames        = runWebSocket(
            port,
            { flow =>
              Source
                .futureSource(
                  closeReceived.future.map { _ =>
                    Source
                      .tick(10.millis, 20.millis, ())
                      .zipWithIndex
                      .map[ExtendedMessage] {
                        case (_, index) if index % 2 == 0 => PingMessage(ByteString("ping"))
                        case _ => PongMessage(ByteString("pong"))
                      }
                  }
                )
                .via(flow)
                .map { frame =>
                  frame match {
                    case SimpleMessage(_: CloseMessage, _) => closeReceived.trySuccess(System.nanoTime())
                    case _                                 =>
                  }
                  frame
                }
                .runWith(consumeFrames)
            },
            acknowledgeServerClose = false
          )
          val closeHandshakeDuration = (System.nanoTime() - await(closeReceived.future)).nanos

          (frames must contain(exactly(closeFrame(CloseCodes.Regular))))
            .and(
              await(consumed.future).exists {
                case _: CloseMessage => true
                case _               => false
              } must beFalse
            )
            .and(closeHandshakeDuration must beLessThan(2.seconds))
        }
      }

      "not send periodic keep-alive frames while awaiting a close acknowledgement" in {
        withServer(
          app =>
            WebSocket.accept[Message, Message] { req =>
              Flow.fromSinkAndSource(Sink.ignore, Source.single(CloseMessage(CloseCodes.Regular)))
            },
          Map(
            "play.server.http.idleTimeout"                       -> "infinite",
            "play.server.https.idleTimeout"                      -> "infinite",
            "play.server.websocket.closeTimeout"                 -> "200 milliseconds",
            "play.server.websocket.periodic-keep-alive-max-idle" -> "20 milliseconds",
            "play.server.websocket.periodic-keep-alive-mode"     -> "ping"
          )
        ) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
            acknowledgeServerClose = false
          )

          frames must contain(exactly(closeFrame(CloseCodes.Regular)))
        }
      }

      "send 1001 and finish graceful server shutdown after the client acknowledges" in {
        val (app, testServer) = createTestServer(app =>
          WebSocket.accept[Message, Message] { _ =>
            Flow.fromSinkAndSource(Sink.ignore, Source.maybe[Message])
          }
        )
        testServer.start()

        try {
          import app.materializer
          val connected = Promise[Unit]()
          val frames    = Future {
            runWebSocket(
              testServer.runningHttpPort.get,
              { flow =>
                val result = Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames)
                connected.success(())
                result
              }
            )
          }

          await(connected.future)
          val serverStopped = Future(testServer.stop())

          (await(frames) must contain(exactly(closeFrame(CloseCodes.GoingAway))))
            .and(await(serverStopped) must beEqualTo(()))
        } finally {
          if (testServer.isRunning) {
            testServer.stop()
          }
        }
      }

      "finish graceful server shutdown after closeTimeout when the client does not acknowledge" in {
        val (app, testServer) = createTestServer(
          app =>
            WebSocket.accept[Message, Message] { _ =>
              Flow.fromSinkAndSource(Sink.ignore, Source.maybe[Message])
            },
          Map(
            "play.server.http.idleTimeout"                       -> "infinite",
            "play.server.https.idleTimeout"                      -> "infinite",
            "play.server.websocket.closeTimeout"                 -> "300 milliseconds",
            "play.server.websocket.periodic-keep-alive-max-idle" -> "infinite"
          )
        )
        testServer.start()

        try {
          import app.materializer
          val connected     = Promise[Unit]()
          val closeReceived = Promise[Long]()
          val frames        = Future {
            runWebSocket(
              testServer.runningHttpPort.get,
              { flow =>
                val result = Source
                  .maybe[ExtendedMessage]
                  .via(flow)
                  .map { frame =>
                    frame match {
                      case SimpleMessage(_: CloseMessage, _) => closeReceived.trySuccess(System.nanoTime())
                      case _                                 =>
                    }
                    frame
                  }
                  .runWith(consumeFrames)
                connected.success(())
                result
              },
              acknowledgeServerClose = false
            )
          }

          await(connected.future)
          val serverStopped = Future(testServer.stop())
          val closeTime     = await(closeReceived.future)

          val receivedFrames         = await(frames)
          val closeHandshakeDuration = (System.nanoTime() - closeTime).nanos
          await(serverStopped)

          (receivedFrames must contain(exactly(closeFrame(CloseCodes.GoingAway))))
            .and(closeHandshakeDuration must be_>=(200.millis))
            .and(closeHandshakeDuration must beLessThan(2.seconds))
        } finally {
          if (testServer.isRunning) {
            testServer.stop()
          }
        }
      }

      "close the websocket with the exception close code when the application source fails" in {
        withServer(app =>
          WebSocket.accept[Message, Message] { req =>
            Flow.fromSinkAndSource(
              Sink.ignore,
              Source.failed[Message](WebSocketCloseException(CloseMessage(4001, "Application close")))
            )
          }
        ) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(4001)
            )
          )
        }
      }

      "close when the consumer is done" in closeWhenTheConsumerIsDone { _ =>
        WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.cancelled, Source.maybe[String]) }
      }

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { _ => statusCode =>
        WebSocket.acceptOrResult[String, String] { req => Future.successful(Left(Results.Status(statusCode))) }
      }

      "allow handling non-upgrade requests with 426 status code" in handleNonUpgradeRequestsGracefully { _ =>
        WebSocket.acceptOrResult[String, String] { req =>
          Future.successful(Left(Results.Status(ACCEPTED))) // The status code is ignored. This code is never reached.
        }
      }

      "aggregate text frames" in {
        val consumed = Promise[List[String]]()
        withServer(app =>
          WebSocket.accept[String, String] { req =>
            Flow.fromSinkAndSource(onFramesConsumed[String](consumed.success(_)), Source.maybe[String])
          }
        ) { (app, port) =>
          import app.materializer
          val result = runWebSocket(
            port,
            { flow =>
              sendFrames(
                TextMessage("first"),
                SimpleMessage(TextMessage("se"), false),
                ContinuationMessage(ByteString("co"), false),
                ContinuationMessage(ByteString("nd"), true),
                TextMessage("third"),
                CloseMessage(1000)
              ).via(flow).runWith(Sink.ignore)
              consumed.future
            }
          )
          result must_== Seq("first", "second", "third")
        }
      }

      "aggregate binary frames" in {
        val consumed = Promise[List[ByteString]]()

        withServer(app =>
          WebSocket.accept[ByteString, ByteString] { req =>
            Flow.fromSinkAndSource(onFramesConsumed[ByteString](consumed.success(_)), Source.maybe[ByteString])
          }
        ) { (app, port) =>
          import app.materializer
          val result = runWebSocket(
            port,
            { flow =>
              sendFrames(
                BinaryMessage(ByteString("first")),
                SimpleMessage(BinaryMessage(ByteString("se")), false),
                ContinuationMessage(ByteString("co"), false),
                ContinuationMessage(ByteString("nd"), true),
                BinaryMessage(ByteString("third")),
                CloseMessage(1000)
              ).via(flow).runWith(Sink.ignore)
              consumed.future
            }
          )
          result.map(b => b.utf8String) must_== Seq("first", "second", "third")
        }
      }

      "reject invalid UTF-8 in a text message" in {
        val (frames, messages) = sendProtocolFrames(RawTextMessage(ByteString(0xc3.toByte, 0x28), true))

        frames must contain(exactly(closeFrame(CloseCodes.InconsistentData)))
        messages must beEmpty
      }

      "reject invalid UTF-8 split across text message fragments" in {
        val (frames, messages) = sendProtocolFrames(
          RawTextMessage(ByteString(0xe2.toByte), false),
          ContinuationMessage(ByteString(0x28), true)
        )

        frames must contain(exactly(closeFrame(CloseCodes.InconsistentData)))
        messages must beEmpty
      }

      "accept valid UTF-8 split across text message fragments" in {
        val (_, messages) = sendProtocolFrames(
          RawTextMessage(ByteString(0xe2.toByte, 0x82.toByte), false),
          ContinuationMessage(ByteString(0xac.toByte), true),
          CloseMessage(CloseCodes.Regular)
        )

        messages must_== List(TextMessage("\u20ac"), CloseMessage(CloseCodes.Regular))
      }

      "allow control frames between data message fragments" in {
        val (frames, messages) = sendProtocolFrames(
          SimpleMessage(TextMessage("hel"), false),
          PingMessage(ByteString("ping")),
          PongMessage(ByteString("pong")),
          ContinuationMessage(ByteString("lo"), true),
          CloseMessage(CloseCodes.Regular)
        )

        frames must contain(exactly(pongFrame(be_==("ping")), closeFrame()).inOrder)
        messages must_== List(
          PingMessage(ByteString("ping")),
          PongMessage(ByteString("pong")),
          TextMessage("hello"),
          CloseMessage(CloseCodes.Regular)
        )
      }

      Seq(
        "Ping"  -> SimpleMessage(PingMessage(ByteString("control")), false),
        "Pong"  -> SimpleMessage(PongMessage(ByteString("control")), false),
        "Close" -> SimpleMessage(CloseMessage(CloseCodes.Regular), false)
      ).foreach {
        case (name, frame) =>
          s"reject a fragmented $name control frame" in {
            val (frames, messages) = sendProtocolFrames(
              frame,
              ContinuationMessage(ByteString("continuation"), true),
              CloseMessage(CloseCodes.Regular)
            )

            frames must contain(exactly(closeFrame(CloseCodes.ProtocolError)))
            messages must beEmpty
          }
      }

      "reject an oversized Pong control frame" in {
        val (frames, messages) =
          sendProtocolFrames(PongMessage(ByteString(Array.fill[Byte](126)('a'.toByte))))

        frames must contain(exactly(closeFrame(CloseCodes.ProtocolError)))
        messages must beEmpty
      }

      "reject an oversized Close control frame" in {
        val (frames, messages) =
          sendProtocolFrames(CloseMessage(CloseCodes.Regular, "a" * 124))

        frames must contain(exactly(closeFrame(CloseCodes.ProtocolError)))
        messages must beEmpty
      }

      "close the websocket when the buffer limit is exceeded" in {
        withServer(app =>
          WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) }
        ) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              sendFrames(
                SimpleMessage(TextMessage("first frame"), false),
                ContinuationMessage(ByteString(new String(Array.fill(65530)('a'))), true)
              ).via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(1009)
            )
          )
        }
      }

      "select one of the subprotocols proposed by the client" in {
        withServer(app =>
          WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source(Nil)) }
        ) { (app, port) =>
          import app.materializer
          val (_, headers) = runWebSocket(
            port,
            { flow =>
              sendFrames(TextMessage("foo"), CloseMessage(1000)).via(flow).runWith(Sink.ignore)
            },
            Some("my_crazy_subprotocol"),
            c => c,
            CompressionMode.Disabled
          )
          (headers
            .map { case (key, value) => (key.toLowerCase, value) }
            .collect { case ("sec-websocket-protocol", selectedProtocol) => selectedProtocol }
            .head must be).equalTo("my_crazy_subprotocol")
        }
      }

      "select the first subprotocol proposed by the client for flow-only handlers" in {
        withServer(app =>
          WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source(Nil)) }
        ) { (app, port) =>
          import app.materializer
          val (_, headers) = runWebSocket(
            port,
            { flow =>
              sendFrames(TextMessage("foo"), CloseMessage(1000)).via(flow).runWith(Sink.ignore)
            },
            Some("first-protocol, second-protocol, third-protocol"),
            c => c,
            CompressionMode.Disabled
          )
          (headers
            .map { case (key, value) => (key.toLowerCase, value) }
            .collect { case ("sec-websocket-protocol", selectedProtocol) => selectedProtocol }
            .head must be).equalTo("first-protocol")
        }
      }

      "allow the application to select a subprotocol proposed by the client" in {
        withServer(app =>
          WebSocket.acceptWithOptions[String, String] { req =>
            WebSocket.Accepted(
              Flow.fromSinkAndSource(Sink.ignore, Source(Nil)),
              Some("graphql-transport-ws")
            )
          }
        ) { (app, port) =>
          import app.materializer
          val (_, headers) = runWebSocket(
            port,
            { flow =>
              sendFrames(TextMessage("foo"), CloseMessage(1000)).via(flow).runWith(Sink.ignore)
            },
            Some("graphql-ws, graphql-transport-ws"),
            c => c,
            CompressionMode.Disabled
          )
          (headers
            .map { case (key, value) => (key.toLowerCase, value) }
            .collect { case ("sec-websocket-protocol", selectedProtocol) => selectedProtocol }
            .head must be).equalTo("graphql-transport-ws")
        }
      }

      "fail the handshake when the application selects a subprotocol not proposed by the client" in {
        val handledError = Promise[Throwable]()
        val errorHandler = new HttpErrorHandler {
          override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
            Future.successful(Results.Status(statusCode)(message))
          }

          override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
            handledError.trySuccess(exception)
            Future.successful(Results.ServiceUnavailable)
          }
        }
        withServer(
          app =>
            WebSocket.acceptWithOptions[String, String] { req =>
              WebSocket.Accepted(
                Flow.fromSinkAndSource(Sink.ignore, Source(Nil)),
                Some("not-offered")
              )
            },
          errorHandler = Some(errorHandler)
        ) { (app, port) =>
          val status    = webSocketHandshakeStatus(app, port, Some("graphql-ws, graphql-transport-ws"))
          val exception = await(handledError.future)

          (status must_== SERVICE_UNAVAILABLE)
            .and(exception must beAnInstanceOf[IllegalArgumentException])
            .and(
              exception.getMessage must_==
                "The application selected WebSocket subprotocol 'not-offered', " +
                "but the client offered [graphql-ws, graphql-transport-ws]"
            )
        }
      }

      "fail the handshake when the application selects a subprotocol but the client proposed none" in {
        withServer(app =>
          WebSocket.acceptWithOptions[String, String] { req =>
            WebSocket.Accepted(
              Flow.fromSinkAndSource(Sink.ignore, Source(Nil)),
              Some("not-offered")
            )
          }
        ) { (app, port) =>
          webSocketHandshakeStatus(app, port, None) must_== INTERNAL_SERVER_ERROR
        }
      }

      "preserve all accepted options when adding handshake headers, cookies, and session data" in {
        withServer(
          app =>
            WebSocket.acceptWithOptions[String, String] { req =>
              val flow: Flow[String, String, ?] =
                Flow.fromSinkAndSource(Sink.ignore, Source.single("plain server message"))
              WebSocket
                .Accepted(flow, Some("graphql-transport-ws"), compressionEnabled = false)
                .withHeaders(
                  "X-WebSocket-Trace" -> "discarded",
                  "x-websocket-trace" -> "scala-trace",
                  "X-Remove"          -> "remove",
                  "Set-Cookie"        -> "scala-raw-cookie=raw-value; Path=/"
                )
                .discardingHeader("x-remove")
                .withCookies(Cookie("scala-ws-cookie", "cookie-value", httpOnly = true))
                .discardingCookies(
                  DiscardingCookie(
                    "scala-expired",
                    secure = true,
                    sameSite = Some(Cookie.SameSite.Lax),
                    partitioned = true
                  )
                )
                .withSession("websocket" -> "connected")
            },
          Map("play.http.session.cookieName" -> "SCALA_SESSION")
        ) { (app, port) =>
          import app.materializer
          val (frames, responseHeaderSeq) = runWebSocket(
            port,
            { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
            Some("graphql-ws, graphql-transport-ws"),
            c => c,
            CompressionMode.RequestOnly()
          )
          val responseHeaders = responseHeaderSeq.map { case (key, value) => (key.toLowerCase, value) }

          responseHeaders.collect { case ("sec-websocket-protocol", value) => value } must contain(
            exactly("graphql-transport-ws")
          )
          responseHeaders.collect { case ("sec-websocket-extensions", value) => value } must beEmpty
          responseHeaders must contain("x-websocket-trace" -> "scala-trace")
          responseHeaders.collect { case ("x-websocket-trace", value) => value } must contain(
            exactly("scala-trace")
          )
          responseHeaders.collect { case ("x-remove", value) => value } must beEmpty
          frames.collectFirst {
            case SimpleMessage(TextMessage(data), true) => data
          } must beSome("plain server message")

          val cookieHeaderEncoding = app.injector.instanceOf[CookieHeaderEncoding]
          val responseCookies      = responseHeaders
            .collect { case ("set-cookie", value) => value }
            .flatMap(cookieHeaderEncoding.decodeSetCookieHeader)
          responseCookies.count(_.name == "scala-raw-cookie") must_== 1
          responseCookies.find(_.name == "scala-raw-cookie").map(_.value) must beSome("raw-value")
          responseCookies.find(_.name == "scala-ws-cookie").map(_.value) must beSome("cookie-value")
          responseCookies.find(_.name == "scala-expired") must beSome[Cookie].like {
            case cookie =>
              (cookie.maxAge must beSome(Cookie.DiscardedMaxAge))
                .and(cookie.secure must beTrue)
                .and(cookie.sameSite must_== Some(Cookie.SameSite.Lax))
                .and(cookie.partitioned must beTrue)
          }

          val sessionBaker = app.injector.instanceOf[SessionCookieBaker]
          sessionBaker.COOKIE_NAME must_== "SCALA_SESSION"
          sessionBaker.decodeFromCookie(responseCookies.find(_.name == sessionBaker.COOKIE_NAME)).data must_== Map(
            "websocket" -> "connected"
          )
        }
      }

      "ignore application supplied WebSocket protocol-owned handshake headers" in {
        withServer(app =>
          WebSocket.acceptWithOptions[String, String] { req =>
            val flow: Flow[String, String, ?] = Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String])
            WebSocket
              .Accepted(flow)
              .withHeaders(
                "Connection"               -> "close",
                "Content-Length"           -> "42",
                "Transfer-Encoding"        -> "chunked",
                "Upgrade"                  -> "h2c",
                "Sec-WebSocket-Accept"     -> "bad-accept",
                "Sec-WebSocket-Extensions" -> "bad-extension",
                "Sec-WebSocket-Protocol"   -> "bad-protocol",
                "Sec-WebSocket-Future"     -> "bad-future",
                "X-WebSocket-Trace"        -> "allowed"
              )
          }
        ) { (app, port) =>
          val responseHeaderSeq = runWebSocket(
            port,
            { flow =>
              sendFrames(TextMessage("foo"), CloseMessage(1000)).via(flow).runWith(Sink.ignore)(app.materializer)
            },
            None,
            c => c,
            CompressionMode.Disabled
          )._2
          val responseHeaders = responseHeaderSeq.map { case (key, value) => (key.toLowerCase, value) }
          responseHeaders must contain("x-websocket-trace" -> "allowed")
          responseHeaders.contains("connection" -> "close") must beFalse
          responseHeaders.contains("content-length" -> "42") must beFalse
          responseHeaders.collect { case ("transfer-encoding", value) => value } must beEmpty
          responseHeaders.contains("upgrade" -> "h2c") must beFalse
          responseHeaders.contains("sec-websocket-accept" -> "bad-accept") must beFalse
          responseHeaders.collect { case ("sec-websocket-extensions", value) => value } must beEmpty
          responseHeaders.collect { case ("sec-websocket-protocol", value) => value } must beEmpty
          responseHeaders.collect { case ("sec-websocket-future", value) => value } must beEmpty
        }
      }

      "reject invalid application supplied WebSocket handshake headers" in {
        withServer(app =>
          WebSocket.acceptWithOptions[String, String] { req =>
            val flow: Flow[String, String, ?] = Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String])
            WebSocket
              .Accepted(flow)
              .withHeaders("X-WebSocket-Trace" -> "trace\r\nInjected: true")
          }
        ) { (app, port) =>
          webSocketHandshakeStatus(app, port, None) must_== INTERNAL_SERVER_ERROR
        }
      }

      "reject invalid application supplied WebSocket handshake cookie headers" in {
        withServer(app =>
          WebSocket.acceptWithOptions[String, String] { req =>
            val flow: Flow[String, String, ?] = Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String])
            WebSocket
              .Accepted(flow)
              .withHeaders("Set-Cookie" -> "websocket=connected\r\nInjected: true")
          }
        ) { (app, port) =>
          webSocketHandshakeStatus(app, port, None) must_== INTERNAL_SERVER_ERROR
        }
      }

      // we keep getting timeouts on this test
      // java.util.concurrent.TimeoutException: Futures timed out after [5 seconds] (Helpers.scala:186)
      "close the websocket when the wrong type of frame is received" in {
        withServer(app =>
          WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) }
        ) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              sendFrames(
                BinaryMessage(ByteString("first")),
                TextMessage("foo")
              ).via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(1003)
            )
          )
        }
      }

      "close a binary websocket when a text frame is received" in {
        withServer(app =>
          WebSocket.accept[ByteString, ByteString] { req =>
            Flow.fromSinkAndSource(Sink.ignore, Source.maybe[ByteString])
          }
        ) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              sendFrames(
                TextMessage("first")
              ).via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(1003)
            )
          )
        }
      }

      "close a JSON websocket when the message is not valid JSON" in {
        withServer(app =>
          WebSocket.accept[JsValue, JsValue] { req =>
            Flow.fromSinkAndSource(Sink.ignore, Source.maybe[JsValue])
          }
        ) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              sendFrames(
                TextMessage("{")
              ).via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(1003)
            )
          )
        }
      }

      "close the websocket with 1003 when JSON validation fails" in {
        withServer(app =>
          WebSocket.accept[JsonMessage, JsValue] { req =>
            Flow.fromSinkAndSource(Sink.ignore, Source.maybe[JsValue])
          }
        ) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              sendFrames(
                TextMessage("""{"unknown":"value"}""")
              ).via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(1003)
            )
          )
        }
      }
    }

    "allow handling a WebSocket with an actor" in {
      "allow consuming messages" in allowConsumingMessages { implicit app => consumed =>
        import app.materializer
        implicit val system = app.actorSystem
        WebSocket.accept[String, String] { req =>
          ActorFlow.actorRef { out =>
            Props(new Actor() {
              var messages = List.empty[String]
              def receive  = {
                case msg: String =>
                  messages = msg :: messages
              }
              override def postStop() = {
                consumed.success(messages.reverse)
              }
            })
          }
        }
      }

      "allow sending messages" in allowSendingMessages { implicit app => messages =>
        import app.materializer
        implicit val system = app.actorSystem
        WebSocket.accept[String, String] { req =>
          ActorFlow.actorRef { out =>
            Props(new Actor() {
              messages.foreach { msg => out ! msg }
              out ! Status.Success(())
              def receive: Actor.Receive = PartialFunction.empty
            })
          }
        }
      }

      "close when the consumer is done" in closeWhenTheConsumerIsDone { implicit app =>
        import app.materializer
        implicit val system = app.actorSystem
        WebSocket.accept[String, String] { req =>
          ActorFlow.actorRef { out =>
            Props(new Actor() {
              system.scheduler.scheduleOnce(10.millis, out, Status.Success(()))
              def receive: Actor.Receive = PartialFunction.empty
            })
          }
        }
      }

      "close when the consumer is terminated" in closeWhenTheConsumerIsDone { implicit app =>
        import app.materializer
        implicit val system = app.actorSystem
        WebSocket.accept[String, String] { req =>
          ActorFlow.actorRef { out =>
            Props(new Actor() {
              def receive = {
                case _ => context.stop(self)
              }
            })
          }
        }
      }

      "clean up when closed" in cleanUpWhenClosed { implicit app => cleanedUp =>
        import app.materializer
        implicit val system = app.actorSystem
        WebSocket.accept[String, String] { req =>
          ActorFlow.actorRef { out =>
            Props(new Actor() {
              def receive: Actor.Receive = PartialFunction.empty
              override def postStop()    = {
                cleanedUp.success(true)
              }
            })
          }
        }
      }

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult {
        implicit app => statusCode =>
          WebSocket.acceptOrResult[String, String] { req => Future.successful(Left(Results.Status(statusCode))) }
      }
    }

    "allow handling a WebSocket in java" in {
      import java.util.{ List => JList }

      import play.core.routing.HandlerInvokerFactory
      import play.core.routing.HandlerInvokerFactory._

      import scala.jdk.CollectionConverters._

      implicit def toHandler[J <: AnyRef](
          javaHandler: => J
      )(implicit factory: HandlerInvokerFactory[J], ct: ClassTag[J]): Handler = {
        val invoker = factory.createInvoker(
          javaHandler,
          HandlerDef(ct.runtimeClass.getClassLoader, "package", "controller", "method", Nil, "GET", "/stream")
        )
        invoker.call(javaHandler)
      }

      "allow consuming messages" in allowConsumingMessages { _ => consumed =>
        val javaConsumed = Promise[JList[String]]()
        consumed.completeWith(javaConsumed.future.map(_.asScala.toList))
        WebSocketSpecJavaActions.allowConsumingMessages(javaConsumed)
      }

      "allow sending messages" in allowSendingMessages { _ => messages =>
        WebSocketSpecJavaActions.allowSendingMessages(messages.asJava)
      }

      "close when the consumer is done" in closeWhenTheConsumerIsDone { _ =>
        WebSocketSpecJavaActions.closeWhenTheConsumerIsDone()
      }

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { _ => statusCode =>
        WebSocketSpecJavaActions.allowRejectingAWebSocketWithAResult(statusCode)
      }

      "allow selecting a subprotocol" in {
        withServer(_ => WebSocketSpecJavaActions.selectSubprotocol()) { (app, port) =>
          import app.materializer
          val (_, headers) = runWebSocket(
            port,
            { flow =>
              sendFrames(TextMessage("foo"), CloseMessage(1000)).via(flow).runWith(Sink.ignore)
            },
            Some("graphql-ws, graphql-transport-ws"),
            c => c,
            CompressionMode.Disabled
          )
          (headers
            .map { case (key, value) => (key.toLowerCase, value) }
            .collect { case ("sec-websocket-protocol", selectedProtocol) => selectedProtocol }
            .head must be).equalTo("graphql-transport-ws")
        }
      }

      "allow selecting a subprotocol while disabling compression" in {
        withServer(_ => WebSocketSpecJavaActions.selectSubprotocolWithoutCompression()) { (app, port) =>
          import app.materializer
          val (frames, headers) = runWebSocket(
            port,
            { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
            Some("graphql-ws, graphql-transport-ws"),
            c => c,
            CompressionMode.RequestOnly()
          )

          (headers
            .map { case (key, value) => (key.toLowerCase, value) }
            .collect { case ("sec-websocket-protocol", selectedProtocol) => selectedProtocol }
            .head must be).equalTo("graphql-transport-ws")
          headers.collectFirst {
            case (name, value) if name.equalsIgnoreCase("Sec-WebSocket-Extensions") => value
          } must beNone
          frames.collectFirst {
            case SimpleMessage(TextMessage(data), true) => data
          } must beSome("plain server message")
        }
      }

      "close a text websocket when a binary frame is received" in {
        withServer(_ => WebSocketSpecJavaActions.acceptText()) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              sendFrames(
                BinaryMessage(ByteString("first"))
              ).via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(1003)
            )
          )
        }
      }

      "close a binary websocket when a text frame is received" in {
        withServer(_ => WebSocketSpecJavaActions.acceptBinary()) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              sendFrames(
                TextMessage("first")
              ).via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(1003)
            )
          )
        }
      }

      "close a JSON websocket when the message is not valid JSON" in {
        withServer(_ => WebSocketSpecJavaActions.acceptJson()) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              sendFrames(
                TextMessage("{")
              ).via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(1003)
            )
          )
        }
      }

      "close a typed JSON websocket when JSON decoding fails" in {
        withServer(_ => WebSocketSpecJavaActions.acceptJsonClass()) { (app, port) =>
          import app.materializer
          val frames = runWebSocket(
            port,
            { flow =>
              sendFrames(
                TextMessage("""{"count":"not-a-number"}""")
              ).via(flow).runWith(consumeFrames)
            }
          )
          frames must contain(
            exactly(
              closeFrame(1003)
            )
          )
        }
      }

      "allow adding handshake headers and cookies" in {
        withServer(
          _ => WebSocketSpecJavaActions.addHandshakeHeadersAndCookies(),
          Map("play.http.session.cookieName" -> "JAVA_SESSION")
        ) { (app, port) =>
          import app.materializer
          val (frames, headers) = runWebSocket(
            port,
            { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) },
            Some("graphql-ws, graphql-transport-ws"),
            c => c,
            CompressionMode.RequestOnly()
          )
          val responseHeaders = headers.map { case (key, value) => (key.toLowerCase, value) }

          responseHeaders.collect { case ("sec-websocket-protocol", value) => value } must contain(
            exactly("graphql-transport-ws")
          )
          responseHeaders.collect { case ("sec-websocket-extensions", value) => value } must beEmpty
          responseHeaders must contain("x-websocket-trace" -> "java-trace")
          responseHeaders.collect { case ("x-websocket-trace", value) => value } must contain(
            exactly("java-trace")
          )
          responseHeaders.collect { case ("x-remove", value) => value } must beEmpty
          frames.collectFirst {
            case SimpleMessage(TextMessage(data), true) => data
          } must beSome("plain server message")

          val cookieHeaderEncoding = app.injector.instanceOf[CookieHeaderEncoding]
          val responseCookies      = responseHeaders
            .collect { case ("set-cookie", value) => value }
            .flatMap(cookieHeaderEncoding.decodeSetCookieHeader)
          responseCookies.count(_.name == "java-raw-cookie") must_== 1
          responseCookies.find(_.name == "java-raw-cookie").map(_.value) must beSome("raw-value")
          responseCookies.find(_.name == "java-ws-cookie").map(_.value) must beSome("cookie-value")
          responseCookies.find(_.name == "java-expired") must beSome[Cookie].like {
            case cookie =>
              (cookie.maxAge must beSome(Cookie.DiscardedMaxAge))
                .and(cookie.secure must beTrue)
                .and(cookie.sameSite must_== Some(Cookie.SameSite.Lax))
                .and(cookie.partitioned must beTrue)
          }

          val sessionBaker = app.injector.instanceOf[SessionCookieBaker]
          sessionBaker.COOKIE_NAME must_== "JAVA_SESSION"
          sessionBaker.decodeFromCookie(responseCookies.find(_.name == sessionBaker.COOKIE_NAME)).data must_== Map(
            "websocket" -> "connected"
          )
        }
      }
    }
  }
}

trait WebSocketSpecMethods extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  import scala.jdk.CollectionConverters._

  // Extend the default spec timeout for CI.
  implicit override def defaultAwaitTimeout: Timeout = 10.seconds

  def failToStartWithInvalidCloseTimeout(value: String) = {
    withServer(
      app => WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) },
      Map("play.server.websocket.closeTimeout" -> value)
    ) { (_, _) =>
      ()
    } must throwA[RuntimeException].like {
      case exception =>
        (exception.getCause must beAnInstanceOf[play.core.server.ServerStartException])
          .and(exception.getMessage must contain("play.server.websocket.closeTimeout"))
    }
  }

  def withServer[A](
      webSocket: Application => Handler,
      extraConfig: Map[String, Any] = Map.empty,
      errorHandler: Option[HttpErrorHandler] = None
  )(
      block: (Application, Int) => A
  ): A = {
    val (app, testServer) = createTestServer(webSocket, extraConfig, errorHandler)
    runningWithPort(testServer)(port => block(app, port))
  }

  def createTestServer(
      webSocket: Application => Handler,
      extraConfig: Map[String, Any] = Map.empty,
      errorHandler: Option[HttpErrorHandler] = None
  ): (Application, play.api.test.TestServer) = {
    val currentApp = new AtomicReference[Application]
    val config     = Configuration(ConfigFactory.parseMap(extraConfig.asJava))
    val builder    = GuiceApplicationBuilder().configure(config)
    val app        = errorHandler
      .fold(builder)(handler => builder.overrides(bind[HttpErrorHandler].to(handler)))
      .routes {
        case _ => webSocket(currentApp.get())
      }
      .build()
    currentApp.set(app)
    val testServer           = TestServer(testServerPort, app)
    val configuredTestServer =
      testServer.copy(config =
        testServer.config.copy(configuration = config.withFallback(testServer.config.configuration))
      )
    app -> configuredTestServer
  }

  def runWebSocket[A](
      port: Int,
      handler: Flow[ExtendedMessage, ExtendedMessage, ?] => Future[A],
      handleConnect: Future[?] => Future[?] = c => c,
      compressionMode: CompressionMode = CompressionMode.Disabled,
      acknowledgeServerClose: Boolean = true
  ): A =
    runWebSocket(port, handler, subprotocol = None, handleConnect, compressionMode, acknowledgeServerClose) match {
      case (result, _) => result
    }

  def runWebSocket[A](
      port: Int,
      handler: Flow[ExtendedMessage, ExtendedMessage, ?] => Future[A],
      subprotocol: Option[String],
      handleConnect: Future[?] => Future[?],
      compressionMode: CompressionMode
  ): (A, immutable.Seq[(String, String)]) =
    runWebSocket(port, handler, subprotocol, handleConnect, compressionMode, acknowledgeServerClose = true)

  def runWebSocket[A](
      port: Int,
      handler: Flow[ExtendedMessage, ExtendedMessage, ?] => Future[A],
      subprotocol: Option[String],
      handleConnect: Future[?] => Future[?],
      compressionMode: CompressionMode,
      acknowledgeServerClose: Boolean
  ): (A, immutable.Seq[(String, String)]) = {
    WebSocketClient { client =>
      val innerResult     = Promise[A]()
      val responseHeaders = Promise[immutable.Seq[(String, String)]]()
      await(
        handleConnect(
          client.connect(
            URI.create("ws://localhost:" + port + "/stream"),
            subprotocol = subprotocol,
            compressionMode = compressionMode,
            acknowledgeServerClose = acknowledgeServerClose
          ) { (headers, flow) =>
            innerResult.completeWith(handler(flow))
            responseHeaders.success(headers)
          }
        )
      )
      (await(innerResult.future), await(responseHeaders.future))
    }
  }

  def inflatePerMessageDeflate(data: ByteString): ByteString = {
    val inflater = new Inflater(true)
    try {
      inflater.setInput((data ++ ByteString(0x00, 0x00, 0xff.toByte, 0xff.toByte)).toArray)
      val output = new ByteArrayOutputStream()
      val buffer = new Array[Byte](256)
      var count  = inflater.inflate(buffer)
      while (count > 0) {
        output.write(buffer, 0, count)
        count = inflater.inflate(buffer)
      }
      ByteString.fromArray(output.toByteArray)
    } finally {
      inflater.end()
    }
  }

  def pongFrame(matcher: Matcher[String]): Matcher[ExtendedMessage] = beLike {
    case SimpleMessage(PongMessage(data), _) => data.utf8String must matcher
  }

  def pingFrame(matcher: Matcher[String]): Matcher[ExtendedMessage] = beLike {
    case SimpleMessage(PingMessage(data), _) => data.utf8String must matcher
  }

  def textFrame(matcher: Matcher[String]): Matcher[ExtendedMessage] = beLike {
    case SimpleMessage(TextMessage(text), _) => text must matcher
  }

  def closeFrame(status: Int = 1000): Matcher[ExtendedMessage] = beLike {
    case SimpleMessage(CloseMessage(statusCode, _), _) => statusCode must beSome(status)
  }

  def closeMessage(status: Int): Matcher[Message] = beLike {
    case CloseMessage(statusCode, _) => statusCode must beSome(status)
  }

  def consumeFrames[A]: Sink[A, Future[List[A]]] =
    Sink.fold[List[A], A](Nil)((result, next) => next :: result).mapMaterializedValue { future =>
      future.map(_.reverse)
    }

  def onFramesConsumed[A](onDone: List[A] => Unit): Sink[A, ?] = consumeFrames[A].mapMaterializedValue { future =>
    future.foreach {
      case list => onDone(list)
    }
  }

  // We concat with an empty source because otherwise the connection will be closed immediately after the last
  // frame is sent, but WebSockets require that the client waits for the server to echo the close back, and
  // let the server close.
  def sendFrames(frames: ExtendedMessage*) = Source(frames.toList).concat(Source.maybe)

  def sendProtocolFrames(frames: ExtendedMessage*): (List[ExtendedMessage], List[Message]) = {
    val consumed = Promise[List[Message]]()
    withServer(app =>
      WebSocket.accept[Message, Message] { req =>
        Flow.fromSinkAndSource(onFramesConsumed[Message](consumed.success(_)), Source.maybe[Message])
      }
    ) { (app, port) =>
      import app.materializer
      runWebSocket(
        port,
        { flow =>
          sendFrames(frames*).via(flow).runWith(consumeFrames).zip(consumed.future)
        }
      )
    }
  }

  /*
   * Shared tests
   */
  def allowConsumingMessages(webSocket: Application => Promise[List[String]] => Handler) = {
    val consumed = Promise[List[String]]()
    withServer(app => webSocket(app)(consumed)) { (app, port) =>
      import app.materializer
      val result = runWebSocket(
        port,
        { flow =>
          sendFrames(
            TextMessage("a"),
            TextMessage("b"),
            CloseMessage(1000)
          ).via(flow).runWith(Sink.cancelled)
          consumed.future
        }
      )
      result must_== Seq("a", "b")
    }
  }

  def allowSendingMessages(webSocket: Application => List[String] => Handler) = {
    withServer(app => webSocket(app)(List("a", "b"))) { (app, port) =>
      import app.materializer
      val frames = runWebSocket(port, { flow => Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames) })
      frames must contain(
        exactly(
          textFrame(be_==("a")),
          textFrame(be_==("b")),
          closeFrame()
        ).inOrder
      )
    }
  }

  def cleanUpWhenClosed(webSocket: Application => Promise[Boolean] => Handler) = {
    val cleanedUp = Promise[Boolean]()
    withServer(app => webSocket(app)(cleanedUp)) { (app, port) =>
      import app.materializer
      runWebSocket(
        port,
        { flow =>
          Source.empty[ExtendedMessage].via(flow).runWith(Sink.ignore)
          cleanedUp.future
        }
      ) must beTrue
    }
  }

  def closeWhenTheConsumerIsDone(webSocket: Application => Handler) = {
    withServer(app => webSocket(app)) { (app, port) =>
      import app.materializer
      val frames = runWebSocket(
        port,
        { flow =>
          Source.repeat[ExtendedMessage](TextMessage("a")).via(flow).runWith(consumeFrames)
        }
      )
      frames must contain(
        exactly(
          closeFrame()
        )
      )
    }
  }

  def allowRejectingTheWebSocketWithAResult(webSocket: Application => Int => Handler) = {
    withServer(app => webSocket(app)(FORBIDDEN)) { (app, port) =>
      webSocketHandshakeStatus(app, port, None) must_== FORBIDDEN
    }
  }

  def webSocketHandshakeStatus(app: Application, port: Int, subprotocol: Option[String]): Int = {
    val ws      = app.injector.instanceOf[WSClient]
    val headers = Seq(
      "Upgrade"               -> "websocket",
      "Connection"            -> "upgrade",
      "Sec-WebSocket-Version" -> "13",
      "Sec-WebSocket-Key"     -> "x3JJHMbDL1EzLkh9GBhXDw==",
      "Origin"                -> "http://example.com"
    ) ++ subprotocol.map("Sec-WebSocket-Protocol" -> _)
    await(
      ws.url(s"http://localhost:$port/stream")
        .addHttpHeaders(headers*)
        .get()
    ).status
  }

  def handleNonUpgradeRequestsGracefully(webSocket: Application => Handler) = {
    withServer(app => webSocket(app)) { (app, port) =>
      val ws = app.injector.instanceOf[WSClient]
      await(
        ws.url(s"http://localhost:$port/stream")
          .addHttpHeaders(
            "Origin" -> "http://example.com"
          )
          .get()
      ).status must_== UPGRADE_REQUIRED
    }
  }

  def delayedSend(
      delay: FiniteDuration,
      idleTimeout: String,
      expectedMessages: Seq[String],
      pekkoHttp2enabled: Boolean = false
  ) = {
    val consumed = Promise[List[String]]()
    withServer(
      app =>
        WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(onFramesConsumed[String](consumed.trySuccess(_)), Source.maybe)
        },
      Map(
        "play.server.pekko.http2.enabled" -> pekkoHttp2enabled,
      ) ++ List("play.server.http.idleTimeout", "play.server.https.idleTimeout")
        .map(_ -> idleTimeout)
    ) { (app, port) =>
      import app.materializer
      // pekko-http abruptly closes the connection (going through onUpstreamFailure), so we have to recover from an IOException
      // netty closes the connection by going through onUpstreamFinish without exception, so no recover needed for it
      val result = runWebSocket(
        port,
        { flow =>
          sendFrames(
            TextMessage("foo"),
            CloseMessage(1000)
          ).delay(delay)
            .via(
              flow.recover(t =>
                // recover from "java.io.IOException: Connection reset by peer"
                consumed.trySuccess(List.empty)
              )
            )
            .runWith(consumeFrames)
          consumed.future
        },
        _.recover(t => ()) // recover from "failed" `disconnected`, see onUpstreamFailure in WebSocketClient
      )
      result must_== expectedMessages // when connection was closed to early, no messages were got send and therefore not consumed
    }
  }

  def handleKeepAlive(
      `periodic-keep-alive-mode`: String,
      `periodic-keep-alive-max-idle`: String,
      sendCloseAfterDelay: FiniteDuration,
      expectedFrames: Seq[Matcher[ExtendedMessage]]
  ) = {
    withServer(
      app => WebSocket.accept[String, String] { req => Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String]) },
      Map(
        "play.server.websocket.periodic-keep-alive-mode"     -> `periodic-keep-alive-mode`,
        "play.server.websocket.periodic-keep-alive-max-idle" -> `periodic-keep-alive-max-idle`,
      )
    ) { (app, port) =>
      import app.materializer
      val frames = runWebSocket(
        port,
        { flow =>
          sendFrames(
            CloseMessage(1000)
          ).delay(sendCloseAfterDelay).via(flow).runWith(consumeFrames)
        }
      )
      frames must contain(exactly((expectedFrames ++ List(closeFrame()))*))
    }
  }
}
