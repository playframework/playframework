/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.websocket

import java.nio.charset.Charset

import play.api.test._
import play.api.Application
import scala.concurrent.{ Future, Promise }
import play.api.mvc.{ Handler, Results, WebSocket }
import play.api.libs.iteratee._
import play.it._
import java.net.URI
import org.jboss.netty.handler.codec.http.websocketx._
import org.specs2.matcher.Matcher
import akka.actor.{ ActorRef, PoisonPill, Actor, Props }
import play.mvc.WebSocket.{ Out, In }
import play.core.routing.HandlerDef
import java.util.concurrent.atomic.AtomicReference
import org.jboss.netty.buffer.ChannelBuffers

import scala.concurrent.ExecutionContext.Implicits.global

object NettyWebSocketSpec extends WebSocketSpec with NettyIntegrationSpecification
object AkkaHttpWebSocketSpec extends WebSocketSpec with AkkaHttpIntegrationSpecification

trait WebSocketSpec extends PlaySpecification with WsTestClient with ServerIntegrationSpecification {

  sequential

  def withServer[A](webSocket: Application => Handler)(block: => A): A = {
    val currentApp = new AtomicReference[FakeApplication]
    val app = FakeApplication(
      withRoutes = {
        case (_, _) => webSocket(currentApp.get())
      }
    )
    currentApp.set(app)
    running(TestServer(testServerPort, app))(block)
  }

  def runWebSocket[A](handler: (Enumerator[WebSocketFrame], Iteratee[WebSocketFrame, _]) => Future[A]): A = {
    val innerResult = Promise[A]()
    WebSocketClient { client =>
      await(client.connect(URI.create("ws://localhost:" + testServerPort + "/stream")) { (in, out) =>
        innerResult.completeWith(handler(in, out))
      })
    }
    await(innerResult.future)
  }

  def pongFrame(matcher: Matcher[String]): Matcher[WebSocketFrame] = beLike {
    case t: PongWebSocketFrame => t.getBinaryData.toString(Charset.forName("utf-8")) must matcher
  }

  def textFrame(matcher: Matcher[String]): Matcher[WebSocketFrame] = beLike {
    case t: TextWebSocketFrame => t.getText must matcher
  }

  def closeFrame(status: Int = 1000): Matcher[WebSocketFrame] = beLike {
    case close: CloseWebSocketFrame => close.getStatusCode must_== status
  }

  def binaryBuffer(text: String) = ChannelBuffers.wrappedBuffer(text.getBytes("utf-8"))

  /**
   * Iteratee getChunks that invokes a callback as soon as it's done.
   */
  def getChunks[A](chunks: List[A], onDone: List[A] => _): Iteratee[A, List[A]] = Cont {
    case Input.El(c) => getChunks(c :: chunks, onDone)
    case Input.EOF =>
      val result = chunks.reverse
      onDone(result)
      Done(result, Input.EOF)
    case Input.Empty => getChunks(chunks, onDone)
  }

  /*
   * Shared tests
   */
  def allowConsumingMessages(webSocket: Application => Promise[List[String]] => Handler) = {
    val consumed = Promise[List[String]]()
    withServer(app => webSocket(app)(consumed)) {
      val result = runWebSocket { (in, out) =>
        Enumerator(new TextWebSocketFrame("a"), new TextWebSocketFrame("b"), new CloseWebSocketFrame(1000, "")) |>>> out
        consumed.future
      }
      result must_== Seq("a", "b")
    }
  }

  def allowSendingMessages(webSocket: Application => List[String] => Handler) = {
    withServer(app => webSocket(app)(List("a", "b"))) {
      val frames = runWebSocket { (in, out) =>
        in |>>> Iteratee.getChunks[WebSocketFrame]
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
    withServer(app => webSocket(app)(cleanedUp)) {
      runWebSocket { (in, out) =>
        out.run
        cleanedUp.future
      } must beTrue
    }
  }

  def closeWhenTheConsumerIsDone(webSocket: Application => Handler) = {
    withServer(app => webSocket(app)) {
      val frames = runWebSocket { (in, out) =>
        Enumerator[WebSocketFrame](new TextWebSocketFrame("foo")) |>> out
        in |>>> Iteratee.getChunks[WebSocketFrame]
      }
      frames must contain(exactly(
        closeFrame()
      ))
    }
  }

  def allowRejectingTheWebSocketWithAResult(webSocket: Application => Int => Handler) = {
    withServer(app => webSocket(app)(FORBIDDEN)) {
      implicit val port = testServerPort
      await(wsUrl("/stream").withHeaders(
        "Upgrade" -> "websocket",
        "Connection" -> "upgrade"
      ).get()).status must_== FORBIDDEN
    }
  }

  "Plays WebSockets" should {
    "allow consuming messages" in allowConsumingMessages { _ =>
      consumed =>
        WebSocket.using[String] { req =>
          (getChunks[String](Nil, consumed.success _), Enumerator.empty)
        }
    }.pendingUntilAkkaHttpFixed // All tests in this class are waiting on https://github.com/akka/akka/issues/16848

    "allow sending messages" in allowSendingMessages { _ =>
      messages =>
        WebSocket.using[String] { req =>
          (Iteratee.ignore, Enumerator.enumerate(messages) >>> Enumerator.eof)
        }
    }.pendingUntilAkkaHttpFixed

    "close when the consumer is done" in closeWhenTheConsumerIsDone { _ =>
      WebSocket.using[String] { req =>
        (Iteratee.head, Enumerator.empty)
      }
    }.pendingUntilAkkaHttpFixed

    "clean up when closed" in cleanUpWhenClosed { _ =>
      cleanedUp =>
        WebSocket.using[String] { req =>
          (Iteratee.ignore, Enumerator.empty[String].onDoneEnumerating(cleanedUp.success(true)))
        }
    }.pendingUntilAkkaHttpFixed

    "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { _ =>
      statusCode =>
        WebSocket.tryAccept[String] { req =>
          Future.successful(Left(Results.Status(statusCode)))
        }
    }.pendingUntilAkkaHttpFixed

    "aggregate text frames" in {
      val consumed = Promise[List[String]]()
      withServer(app => WebSocket.using[String] { req =>
        (getChunks[String](Nil, consumed.success _), Enumerator.empty)
      }) {
        val result = runWebSocket { (in, out) =>
          Enumerator(
            new TextWebSocketFrame("first"),
            new TextWebSocketFrame(false, 0, "se"),
            new ContinuationWebSocketFrame(false, 0, "co"),
            new ContinuationWebSocketFrame(true, 0, "nd"),
            new TextWebSocketFrame("third"),
            new CloseWebSocketFrame(1000, "")) |>>> out
          consumed.future
        }
        result must_== Seq("first", "second", "third")
      }

    }.pendingUntilAkkaHttpFixed

    "aggregate binary frames" in {
      val consumed = Promise[List[Array[Byte]]]()

      withServer(app => WebSocket.using[Array[Byte]] { req =>
        (getChunks[Array[Byte]](Nil, consumed.success _), Enumerator.empty)
      }) {
        val result = runWebSocket { (in, out) =>
          Enumerator(
            new BinaryWebSocketFrame(binaryBuffer("first")),
            new BinaryWebSocketFrame(false, 0, binaryBuffer("se")),
            new ContinuationWebSocketFrame(false, 0, binaryBuffer("co")),
            new ContinuationWebSocketFrame(true, 0, binaryBuffer("nd")),
            new BinaryWebSocketFrame(binaryBuffer("third")),
            new CloseWebSocketFrame(1000, "")) |>>> out
          consumed.future
        }
        result.map(b => b.toSeq) must_== Seq("first".getBytes("utf-8").toSeq, "second".getBytes("utf-8").toSeq, "third".getBytes("utf-8").toSeq)
      }
    }.pendingUntilAkkaHttpFixed

    "close the websocket when the buffer limit is exceeded" in {
      withServer(app => WebSocket.using[String] { req =>
        (Iteratee.ignore, Enumerator.empty)
      }) {
        val frames = runWebSocket { (in, out) =>
          Enumerator[WebSocketFrame](
            new TextWebSocketFrame(false, 0, "first frame"),
            new ContinuationWebSocketFrame(true, 0, new String(Array.range(1, 65530).map(_ => 'a')))
          ) |>> out
          in |>>> Iteratee.getChunks[WebSocketFrame]
        }
        frames must contain(exactly(
          closeFrame(1009)
        ))
      }
    }.pendingUntilAkkaHttpFixed

    "close the websocket when the wrong type of frame is received" in {
      withServer(app => WebSocket.using[Array[Byte]] { req =>
        (Iteratee.ignore, Enumerator.empty)
      }) {
        val frames = runWebSocket { (in, out) =>
          Enumerator[WebSocketFrame](
            new BinaryWebSocketFrame(binaryBuffer("first")),
            new TextWebSocketFrame("foo")) |>> out
          in |>>> Iteratee.getChunks[WebSocketFrame]
        }
        frames must contain(exactly(
          closeFrame(1003)
        ))
      }
    }.pendingUntilAkkaHttpFixed

    "respond to pings" in {
      withServer(app => WebSocket.using[String] { req =>
        (Iteratee.head, Enumerator.empty)
      }) {
        val frames = runWebSocket { (in, out) =>
          Enumerator[WebSocketFrame](
            new PingWebSocketFrame(binaryBuffer("hello")),
            new CloseWebSocketFrame(1000, "")
          ) |>> out
          in |>>> Iteratee.getChunks[WebSocketFrame]
        }
        frames must contain(exactly(
          pongFrame(be_==("hello")),
          closeFrame()
        ))
      }
    }.pendingUntilAkkaHttpFixed

    "not respond to pongs" in {
      withServer(app => WebSocket.using[String] { req =>
        (Iteratee.head, Enumerator.empty)
      }) {
        val frames = runWebSocket { (in, out) =>
          Enumerator[WebSocketFrame](
            new PongWebSocketFrame(),
            new CloseWebSocketFrame(1000, "")
          ) |>> out
          in |>>> Iteratee.getChunks[WebSocketFrame]
        }
        frames must contain(exactly(
          closeFrame()
        ))
      }
    }.pendingUntilAkkaHttpFixed

    "allow handling a WebSocket with an actor" in {

      "allow consuming messages" in allowConsumingMessages { implicit app =>
        consumed =>
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
      }.pendingUntilAkkaHttpFixed

      "allow sending messages" in allowSendingMessages { implicit app =>
        messages =>
          WebSocket.acceptWithActor[String, String] { req =>
            out =>
              Props(new Actor() {
                messages.foreach { msg =>
                  out ! msg
                }
                out ! PoisonPill
                def receive = PartialFunction.empty
              })
          }
      }.pendingUntilAkkaHttpFixed

      "close when the consumer is done" in closeWhenTheConsumerIsDone { implicit app =>
        WebSocket.acceptWithActor[String, String] { req =>
          out =>
            Props(new Actor() {
              out ! PoisonPill
              def receive = PartialFunction.empty
            })
        }
      }.pendingUntilAkkaHttpFixed

      "clean up when closed" in cleanUpWhenClosed { implicit app =>
        cleanedUp =>
          WebSocket.acceptWithActor[String, String] { req =>
            out =>
              Props(new Actor() {
                def receive = PartialFunction.empty
                override def postStop() = {
                  cleanedUp.success(true)
                }
              })
          }
      }.pendingUntilAkkaHttpFixed

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { implicit app =>
        statusCode =>
          WebSocket.tryAcceptWithActor[String, String] { req =>
            Future.successful(Left(Results.Status(statusCode)))
          }
      }.pendingUntilAkkaHttpFixed

    }

    "allow handling a WebSocket in java" in {

      import play.core.routing.HandlerInvokerFactory
      import play.core.routing.HandlerInvokerFactory._
      import play.mvc.{ WebSocket => JWebSocket, Results => JResults }
      import play.libs.F

      implicit def toHandler[J <: AnyRef](javaHandler: J)(implicit factory: HandlerInvokerFactory[J]): Handler = {
        val invoker = factory.createInvoker(
          javaHandler,
          new HandlerDef(javaHandler.getClass.getClassLoader, "package", "controller", "method", Nil, "GET", "", "/stream")
        )
        invoker.call(javaHandler)
      }

      "allow consuming messages" in allowConsumingMessages { _ =>
        consumed =>
          new JWebSocket[String] {
            @volatile var messages = List.empty[String]
            def onReady(in: In[String], out: Out[String]) = {
              in.onMessage(new F.Callback[String] {
                def invoke(msg: String) = messages = msg :: messages
              })
              in.onClose(new F.Callback0 {
                def invoke() = consumed.success(messages.reverse)
              })
            }
          }
      }.pendingUntilAkkaHttpFixed

      "allow sending messages" in allowSendingMessages { _ =>
        messages =>
          new JWebSocket[String] {
            def onReady(in: In[String], out: Out[String]) = {
              messages.foreach { msg =>
                out.write(msg)
              }
              out.close()
            }
          }
      }.pendingUntilAkkaHttpFixed

      "clean up when closed" in cleanUpWhenClosed { _ =>
        cleanedUp =>
          new JWebSocket[String] {
            def onReady(in: In[String], out: Out[String]) = {
              in.onClose(new F.Callback0 {
                def invoke() = cleanedUp.success(true)
              })
            }
          }
      }.pendingUntilAkkaHttpFixed

      "allow rejecting a websocket with a result" in allowRejectingTheWebSocketWithAResult { _ =>
        statusCode =>
          JWebSocket.reject[String](JResults.status(statusCode))
      }.pendingUntilAkkaHttpFixed

      "allow handling a websocket with an actor" in allowSendingMessages { _ =>
        messages =>
          JWebSocket.withActor[String](new F.Function[ActorRef, Props]() {
            def apply(out: ActorRef) = {
              Props(new Actor() {
                messages.foreach { msg =>
                  out ! msg
                }
                out ! PoisonPill
                def receive = PartialFunction.empty
              })
            }
          })
      }.pendingUntilAkkaHttpFixed
    }
  }
}
