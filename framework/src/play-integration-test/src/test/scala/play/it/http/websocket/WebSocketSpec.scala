/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http.websocket

import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import java.util.function.{Consumer, Function}

import akka.actor._
import akka.stream.scaladsl._
import akka.util.ByteString
import org.specs2.matcher.Matcher
import play.api.Application
import play.api.http.websocket._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.iteratee._
import play.api.mvc.{Handler, Results, WebSocket}
import play.api.test._
import play.core.routing.HandlerDef
import play.it._
import play.it.http.websocket.WebSocketClient.{ContinuationMessage, ExtendedMessage, SimpleMessage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.reflect.ClassTag

object NettyWebSocketSpec extends WebSocketSpec with NettyIntegrationSpecification
object AkkaHttpWebSocketSpec extends WebSocketSpec with AkkaHttpIntegrationSpecification

trait WebSocketSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  sequential

  override implicit def defaultAwaitTimeout = 5.seconds

  def withServer[A](webSocket: Application => Handler)(block: Application => A): A = {
    val currentApp = new AtomicReference[Application]
    val app = GuiceApplicationBuilder().routes {
      case _ => webSocket(currentApp.get())
    }.build()
    currentApp.set(app)
    running(TestServer(testServerPort, app))(block(app))
  }

  def runWebSocket[A](handler: (Flow[ExtendedMessage, ExtendedMessage, _]) => Future[A]): A = {
    WebSocketClient { client =>
      val innerResult = Promise[A]()
      await(client.connect(URI.create("ws://localhost:" + testServerPort + "/stream")) { flow =>
        innerResult.completeWith(handler(flow))
      })
      await(innerResult.future)
    }
  }

  def pongFrame(matcher: Matcher[String]): Matcher[ExtendedMessage] = beLike {
    case SimpleMessage(PongMessage(data), _) => data.utf8String must matcher
  }

  def textFrame(matcher: Matcher[String]): Matcher[ExtendedMessage] = beLike {
    case SimpleMessage(TextMessage(text), _) => text must matcher
  }

  def closeFrame(status: Int = 1000): Matcher[ExtendedMessage] = beLike {
    case SimpleMessage(CloseMessage(statusCode, _), _) => statusCode must beSome(status)
  }

  def consumeFrames[A]: Sink[A, Future[List[A]]] =
    Sink.fold[List[A], A](Nil)((result, next) => next :: result).mapMaterializedValue { future =>
      future.map(_.reverse)
    }

  def onFramesConsumed[A](onDone: List[A] => Unit): Sink[A, _] = consumeFrames[A].mapMaterializedValue { future =>
    future.onSuccess {
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
    withServer(app => webSocket(app)(consumed)) { app =>
      import app.materializer
      val result = runWebSocket { (flow) =>
        sendFrames(
          TextMessage("a"),
          TextMessage("b"),
          CloseMessage(1000)
        ).via(flow).runWith(Sink.cancelled)
        consumed.future
      }
      result must_== Seq("a", "b")
    }
  }

  def allowSendingMessages(webSocket: Application => List[String] => Handler) = {
    withServer(app => webSocket(app)(List("a", "b"))) { app =>
      import app.materializer
      val frames = runWebSocket { (flow) =>
        Source.maybe[ExtendedMessage].via(flow).runWith(consumeFrames)
      }
      frames must contain(exactly(
        textFrame(be_==("a")),
        textFrame(be_==("b")),
        closeFrame()
      ).inOrder)
    }
  }

  def cleanUpWhenClosed(webSocket: Application => Promise[Boolean] => Handler) = {
    val cleanedUp = Promise[Boolean]()
    withServer(app => webSocket(app)(cleanedUp)) { app =>
      import app.materializer
      runWebSocket { flow =>
        Source.empty[ExtendedMessage].via(flow).runWith(Sink.ignore)
        cleanedUp.future
      } must beTrue
    }
  }

  def closeWhenTheConsumerIsDone(webSocket: Application => Handler) = {
    withServer(app => webSocket(app)) { app =>
      import app.materializer
      val frames = runWebSocket { flow =>
        Source.repeat[ExtendedMessage](TextMessage("a")).via(flow).runWith(consumeFrames)
      }
      frames must contain(exactly(
        closeFrame()
      ))
    }
  }

  def allowRejectingTheWebSocketWithAResult(webSocket: Application => Int => Handler) = {
    withServer(app => webSocket(app)(FORBIDDEN)) { app =>
      implicit val port = testServerPort
      await(wsUrl("/stream").withHeaders(
        "Upgrade" -> "websocket",
        "Connection" -> "upgrade",
        "Sec-WebSocket-Version" -> "13",
        "Sec-WebSocket-Key" -> "x3JJHMbDL1EzLkh9GBhXDw==",
        "Origin" -> "http://example.com"
      ).get()).status must_== FORBIDDEN
    }
  }

  "Plays WebSockets" should {
    "allow handling WebSockets using Akka streams" in {
      "allow consuming messages" in allowConsumingMessages { _ =>
        consumed =>
          WebSocket.accept[String, String] { req =>
            Flow.fromSinkAndSource(onFramesConsumed[String](consumed.success(_)),
              Source.maybe[String])
          }
      }

      "allow sending messages" in allowSendingMessages { _ =>
        messages =>
          WebSocket.accept[String, String] { req =>
            Flow.fromSinkAndSource(Sink.ignore, Source(messages))
          }
      }

      "close when the consumer is done" in closeWhenTheConsumerIsDone { _ =>
        WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(Sink.cancelled, Source.maybe[String])
        }
      }

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { _ =>
        statusCode =>
          WebSocket.acceptOrResult[String, String] { req =>
            Future.successful(Left(Results.Status(statusCode)))
          }
      }

      "aggregate text frames" in {
        val consumed = Promise[List[String]]()
        withServer(app => WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(onFramesConsumed[String](consumed.success(_)),
            Source.maybe[String])
        }) { app =>
          import app.materializer
          val result = runWebSocket { flow =>
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
          result must_== Seq("first", "second", "third")
        }
      }

      "aggregate binary frames" in {
        val consumed = Promise[List[ByteString]]()

        withServer(app => WebSocket.accept[ByteString, ByteString] { req =>
          Flow.fromSinkAndSource(onFramesConsumed[ByteString](consumed.success(_)),
            Source.maybe[ByteString])
        }) { app =>
          import app.materializer
          val result = runWebSocket { flow =>
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
          result.map(b => b.utf8String) must_== Seq("first", "second", "third")
        }
      }

      "close the websocket when the buffer limit is exceeded" in {
        withServer(app => WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String])
        }) { app =>
          import app.materializer
          val frames = runWebSocket { flow =>
            sendFrames(
              SimpleMessage(TextMessage("first frame"), false),
              ContinuationMessage(ByteString(new String(Array.range(1, 65530).map(_ => 'a'))), true)
            ).via(flow).runWith(consumeFrames)
          }
          frames must contain(exactly(
            closeFrame(1009)
          ))
        }
      }

      "close the websocket when the wrong type of frame is received" in {
        withServer(app => WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String])
        }) { app =>
          import app.materializer
          val frames = runWebSocket { flow =>
            sendFrames(
              BinaryMessage(ByteString("first")),
              TextMessage("foo")
            ).via(flow).runWith(consumeFrames)
          }
          frames must contain(exactly(
            closeFrame(1003)
          ))
        }
      }

      "respond to pings" in {
        withServer(app => WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String])
        }) { app =>
          import app.materializer
          val frames = runWebSocket { flow =>
            sendFrames(
              PingMessage(ByteString("hello")),
              CloseMessage(1000)
            ).via(flow).runWith(consumeFrames)
          }
          frames must contain(exactly(
            pongFrame(be_==("hello")),
            closeFrame()
          ))
        }
      }

      "not respond to pongs" in {
        withServer(app => WebSocket.accept[String, String] { req =>
          Flow.fromSinkAndSource(Sink.ignore, Source.maybe[String])
        }) { app =>
          import app.materializer
          val frames = runWebSocket { flow =>
            sendFrames(
              PongMessage(ByteString("hello")),
              CloseMessage(1000)
            ).via(flow).runWith(consumeFrames)
          }
          frames must contain(exactly(
            closeFrame()
          ))
        }
      }

    }

    "allow handling WebSockets using iteratees" in {

      "allow consuming messages" in allowConsumingMessages { _ =>
        consumed =>
          WebSocket.using[String] { req =>
            (Iteratee.getChunks[String].map { result =>
              consumed.success(result)
            }, Enumerator.empty)
          }
      }

      "allow sending messages" in allowSendingMessages { _ =>
        messages =>
          WebSocket.using[String] { req =>
            (Iteratee.ignore, Enumerator.enumerate(messages) >>> Enumerator.eof)
          }
      }

      "close when the consumer is done" in closeWhenTheConsumerIsDone { _ =>
        WebSocket.using[String] { req =>
          (Done(()), Enumerator.empty)
        }
      }

      "clean up when closed" in cleanUpWhenClosed { app =>
        cleanedUp =>
          WebSocket.using[String] { req =>
            val tick = Enumerator.unfoldM(()) { _ =>
              val p = Promise[Option[(Unit, String)]]()
              app.actorSystem.scheduler.scheduleOnce(100.millis)(p.success(Some(() -> "foo")))
              p.future
            }
            (Iteratee.ignore, tick.onDoneEnumerating {
              cleanedUp.success(true)
            })
          }
      }

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { _ =>
        statusCode =>
          WebSocket.tryAccept[String] { req =>
            Future.successful(Left(Results.Status(statusCode)))
          }
      }
    }

    "allow handling a WebSocket with an actor" in {

      "allow consuming messages" in allowConsumingMessages { implicit app =>
        consumed =>
          import app.materializer
          WebSocket.acceptWithActor[String, String] { req =>
            out =>
              Props(new Actor() {
                var messages = List.empty[String]
                def receive = {
                  case msg: String =>
                    messages = msg :: messages
                }
                override def postStop() = {
                  consumed.success(messages.reverse)
                }
              })
          }
      }

      "allow sending messages" in allowSendingMessages { implicit app =>
        messages =>
          import app.materializer
          WebSocket.acceptWithActor[String, String] { req =>
            out =>
              Props(new Actor() {
                messages.foreach { msg =>
                  out ! msg
                }
                out ! Status.Success(())
                def receive = PartialFunction.empty
              })
          }
      }

      "close when the consumer is done" in closeWhenTheConsumerIsDone { implicit app =>
        import app.materializer
        WebSocket.acceptWithActor[String, String] { req =>
          out =>
            Props(new Actor() {
              out ! Status.Success(())
              def receive = PartialFunction.empty
            })
        }
      }

      "clean up when closed" in cleanUpWhenClosed { implicit app =>
        cleanedUp =>
          import app.materializer
          WebSocket.acceptWithActor[String, String] { req =>
            out =>
              Props(new Actor() {
                def receive = PartialFunction.empty
                override def postStop() = {
                  cleanedUp.success(true)
                }
              })
          }
      }

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { implicit app =>
        statusCode =>
          import app.materializer
          WebSocket.tryAcceptWithActor[String, String] { req =>
            Future.successful(Left(Results.Status(statusCode)))
          }
      }

    }

    "allow handling a WebSocket in java" in {

      import java.util.{List => JList}

      import play.core.routing.HandlerInvokerFactory
      import play.core.routing.HandlerInvokerFactory._

      import scala.collection.JavaConverters._

      implicit def toHandler[J <: AnyRef](javaHandler: => J)(implicit factory: HandlerInvokerFactory[J], ct: ClassTag[J]): Handler = {
        val invoker = factory.createInvoker(
          javaHandler,
          new HandlerDef(ct.runtimeClass.getClassLoader, "package", "controller", "method", Nil, "GET", "", "/stream")
        )
        invoker.call(javaHandler)
      }

      "allow consuming messages" in allowConsumingMessages { _ =>
        consumed =>
          val javaConsumed = Promise[JList[String]]()
          consumed.completeWith(javaConsumed.future.map(_.asScala.toList))
          WebSocketSpecJavaActions.allowConsumingMessages(javaConsumed)
      }

      "allow sending messages" in allowSendingMessages { _ =>
        messages =>
          WebSocketSpecJavaActions.allowSendingMessages(messages.asJava)
      }

      "close when the consumer is done" in closeWhenTheConsumerIsDone { _ =>
        WebSocketSpecJavaActions.closeWhenTheConsumerIsDone()
      }

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { _ =>
        statusCode =>
          WebSocketSpecJavaActions.allowRejectingAWebSocketWithAResult(statusCode)
      }

    }

    "allow handling a WebSocket using legacy java API" in {

      import play.core.routing.HandlerInvokerFactory
      import play.core.routing.HandlerInvokerFactory._
      import play.mvc.{LegacyWebSocket, Results => JResults, WebSocket => JWebSocket}
      import JWebSocket.{In, Out}

      implicit def toHandler[J <: AnyRef](javaHandler: J)(implicit factory: HandlerInvokerFactory[J]): Handler = {
        val invoker = factory.createInvoker(
          javaHandler,
          new HandlerDef(javaHandler.getClass.getClassLoader, "package", "controller", "method", Nil, "GET", "", "/stream")
        )
        invoker.call(javaHandler)
      }

      "allow consuming messages" in allowConsumingMessages { _ =>
        consumed =>
          new LegacyWebSocket[String] {
            @volatile var messages = List.empty[String]
            def onReady(in: In[String], out: Out[String]) = {
              in.onMessage(new Consumer[String] {
                def accept(msg: String) = messages = msg :: messages
              })
              in.onClose(new Runnable {
                def run() = consumed.success(messages.reverse)
              })
            }
          }
      }

      "allow sending messages" in allowSendingMessages { _ =>
        messages =>
          new LegacyWebSocket[String] {
            def onReady(in: In[String], out: Out[String]) = {
              messages.foreach { msg =>
                out.write(msg)
              }
              out.close()
            }
          }
      }

      "clean up when closed" in cleanUpWhenClosed { _ =>
        cleanedUp =>
          new LegacyWebSocket[String] {
            def onReady(in: In[String], out: Out[String]) = {
              in.onClose(new Runnable {
                def run() = cleanedUp.success(true)
              })
            }
          }
      }

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { _ =>
        statusCode =>
          JWebSocket.reject[String](JResults.status(statusCode))
      }

      "allow handling a websocket with an actor" in allowSendingMessages { _ =>
        messages =>


          JWebSocket.withActor[String](new Function[ActorRef, Props]() {
            def apply(out: ActorRef) = {
              Props(new Actor() {
                messages.foreach { msg =>
                  out ! msg
                }
                out ! Status.Success(())
                def receive ={
                  case msg: Message => ()
                }
              })
            }
          })
      }
    }

  }
}
