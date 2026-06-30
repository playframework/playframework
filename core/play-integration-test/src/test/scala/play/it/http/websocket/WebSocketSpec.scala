/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.websocket

import java.net.URI
import java.util.concurrent.atomic.AtomicReference

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
import org.apache.pekko.util.ByteString
import org.apache.pekko.util.Timeout
import org.specs2.execute.AsResult
import org.specs2.execute.EventuallyResults
import org.specs2.matcher.Matcher
import org.specs2.specification.AroundEach
import play.api.http.websocket._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc.Handler
import play.api.mvc.Results
import play.api.mvc.WebSocket
import play.api.routing.HandlerDef
import play.api.test._
import play.api.Application
import play.api.Configuration
import play.it._
import play.it.http.websocket.WebSocketClient.CompressionMode
import play.it.http.websocket.WebSocketClient.ContinuationMessage
import play.it.http.websocket.WebSocketClient.ExtendedMessage
import play.it.http.websocket.WebSocketClient.RawWebSocketFrame
import play.it.http.websocket.WebSocketClient.SimpleMessage

class NettyWebSocketSpec extends WebSocketSpec with NettyIntegrationSpecification {
  "Plays WebSockets using netty backend with compression" should {
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
          compressionMode = CompressionMode.RequestOnly
        )

        frames.collectFirst {
          case RawWebSocketFrame("text", data, rsv, true) => (data, rsv)
        } must beSome[(ByteString, Int)].which {
          case (data, rsv) =>
            data.nonEmpty && (rsv & 4) == 4
        }
      }
    }
  }
}
class PekkoHttpWebSocketSpec extends WebSocketSpec with PekkoHttpIntegrationSpecification {
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
    }
  }
}

trait WebSocketSpecMethods extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  import scala.jdk.CollectionConverters._

  // Extend the default spec timeout for CI.
  implicit override def defaultAwaitTimeout: Timeout = 10.seconds

  def withServer[A](webSocket: Application => Handler, extraConfig: Map[String, Any] = Map.empty)(
      block: (Application, Int) => A
  ): A = {
    val currentApp = new AtomicReference[Application]
    val config     = Configuration(ConfigFactory.parseMap(extraConfig.asJava))
    val app        = GuiceApplicationBuilder()
      .configure(config)
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
    runningWithPort(configuredTestServer)(port => block(app, port))
  }

  def runWebSocket[A](
      port: Int,
      handler: Flow[ExtendedMessage, ExtendedMessage, ?] => Future[A],
      handleConnect: Future[?] => Future[?] = c => c,
      compressionMode: CompressionMode = CompressionMode.Disabled
  ): A =
    runWebSocket(port, handler, subprotocol = None, handleConnect, compressionMode) match { case (result, _) => result }

  def runWebSocket[A](
      port: Int,
      handler: Flow[ExtendedMessage, ExtendedMessage, ?] => Future[A],
      subprotocol: Option[String],
      handleConnect: Future[?] => Future[?],
      compressionMode: CompressionMode
  ): (A, immutable.Seq[(String, String)]) = {
    WebSocketClient { client =>
      val innerResult     = Promise[A]()
      val responseHeaders = Promise[immutable.Seq[(String, String)]]()
      await(
        handleConnect(
          client.connect(
            URI.create("ws://localhost:" + port + "/stream"),
            subprotocol = subprotocol,
            compressionMode = compressionMode
          ) { (headers, flow) =>
            innerResult.completeWith(handler(flow))
            responseHeaders.success(headers)
          }
        )
      )
      (await(innerResult.future), await(responseHeaders.future))
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
      val ws = app.injector.instanceOf[WSClient]
      await(
        ws.url(s"http://localhost:$port/stream")
          .addHttpHeaders(
            "Upgrade"               -> "websocket",
            "Connection"            -> "upgrade",
            "Sec-WebSocket-Version" -> "13",
            "Sec-WebSocket-Key"     -> "x3JJHMbDL1EzLkh9GBhXDw==",
            "Origin"                -> "http://example.com"
          )
          .get()
      ).status must_== FORBIDDEN
    }
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
