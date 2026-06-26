/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.apache.pekko.Done
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import play.api.http.websocket.CloseCodes
import play.api.http.websocket.CloseMessage
import play.api.http.websocket.Message

class WebSocketFlowHandlerSpec extends Specification {
  "WebSocketFlowHandler" should {
    "send 1006 to the application when remote input fails without a close frame" in withActorSystem {
      implicit materializer =>
        val messages = runFailedRemoteInput(new RuntimeException("connection failed"))

        messages must contain(exactly(closeMessage(CloseCodes.ConnectionAbort)))
    }

    "forward an invalid close status code reported by the backend failure" in withActorSystem { implicit materializer =>
      val messages = runFailedRemoteInput(new RuntimeException("Invalid close frame getStatus code: 1006"))

      messages must contain(exactly(closeMessage(CloseCodes.ConnectionAbort)))
    }

    "not send 1006 when the application initiated close and remote input then fails" in withActorSystem {
      implicit materializer =>
        val messages = runAppInitiatedCloseThenFailedRemoteInput()

        messages must beEmpty
    }

    "forward a remote close code without sending 1006" in withActorSystem { implicit materializer =>
      val messages = runRemoteInput(rawClose(CloseCodes.GoingAway))

      messages must contain(exactly(closeMessage(CloseCodes.GoingAway)))
    }

    "send 1006 after remote input completes even when the application has no immediate demand" in withActorSystem {
      implicit materializer =>
        val messages = runCompletedRemoteInputWithoutInitialAppDemand()

        messages must contain(exactly(closeMessage(CloseCodes.ConnectionAbort)))
    }
  }

  private def runFailedRemoteInput(ex: Throwable)(implicit materializer: Materializer): Seq[Message] = {
    runRemoteInput(Source.failed[WebSocketFlowHandler.RawMessage](ex))
  }

  private def runRemoteInput(messages: WebSocketFlowHandler.RawMessage*)(
      implicit materializer: Materializer
  ): Seq[Message] = {
    runRemoteInput(Source(messages.toList))
  }

  private def runRemoteInput(
      remoteIn: Source[WebSocketFlowHandler.RawMessage, ?]
  )(implicit materializer: Materializer): Seq[Message] = {
    runRemoteInputWithAppSource(remoteIn, Source.maybe[Message])
  }

  private def runRemoteInputWithAppSource(
      remoteIn: Source[WebSocketFlowHandler.RawMessage, ?],
      appIn: Source[Message, ?]
  )(implicit materializer: Materializer): Seq[Message] = {
    val appFlow = Flow.fromSinkAndSourceMat(Sink.seq[Message], appIn)(Keep.left)
    val flow    = protocol.joinMat(appFlow)(Keep.right)

    val (appMessages, remoteOutDone) = remoteIn.viaMat(flow)(Keep.right).toMat(Sink.ignore)(Keep.both).run()

    awaitRemoteOut(remoteOutDone)
    Await.result(appMessages, 5.seconds)
  }

  private def runAppInitiatedCloseThenFailedRemoteInput()(implicit materializer: Materializer): Seq[Message] = {
    val appFlow = Flow.fromSinkAndSourceMat(
      Sink.seq[Message],
      Source.single(CloseMessage(CloseCodes.Regular))
    )(Keep.left)
    val flow      = protocol.joinMat(appFlow)(Keep.right)
    val closeSent = Promise[Message]()

    val ((remoteIn, appMessages), remoteOutDone) =
      Source
        .queue[WebSocketFlowHandler.RawMessage](1)
        .viaMat(flow)(Keep.both)
        .toMat(Sink.foreach[Message](message => closeSent.trySuccess(message)))(Keep.both)
        .run()

    Await.result(closeSent.future, 5.seconds) must beLike { case CloseMessage(Some(CloseCodes.Regular), _) => ok }
    remoteIn.fail(new RuntimeException("connection failed"))
    awaitRemoteOut(remoteOutDone)
    Await.result(appMessages, 5.seconds)
  }

  private def runCompletedRemoteInputWithoutInitialAppDemand()(
      implicit materializer: Materializer
  ): Seq[Message] = {
    val appFlow = Flow.fromSinkAndSourceMat(Sink.asPublisher[Message](fanout = false), Source.maybe[Message])(Keep.left)
    val flow    = protocol.joinMat(appFlow)(Keep.right)

    val (appPublisher, remoteOutDone) =
      Source.empty[WebSocketFlowHandler.RawMessage].viaMat(flow)(Keep.right).toMat(Sink.ignore)(Keep.both).run()

    val appMessages = Source.fromPublisher(appPublisher).runWith(Sink.seq)

    awaitRemoteOut(remoteOutDone)
    Await.result(appMessages, 5.seconds)
  }

  private def protocol =
    WebSocketFlowHandler.webSocketProtocol(
      bufferLimit = 65536,
      wsKeepAliveMode = "ping",
      wsKeepAliveMaxIdle = Duration.Inf
    )

  private def rawClose(statusCode: Int): WebSocketFlowHandler.RawMessage = {
    WebSocketFlowHandler.RawMessage(
      WebSocketFlowHandler.MessageType.Close,
      ByteString((statusCode >> 8).toByte, statusCode.toByte),
      isFinal = true
    )
  }

  private def awaitRemoteOut(done: Future[Done]): Unit = {
    Await.result(done.recover { case _ => Done }(scala.concurrent.ExecutionContext.parasitic), 5.seconds)
  }

  private def closeMessage(status: Int): Matcher[Message] = beLike {
    case CloseMessage(statusCode, _) => statusCode must beSome(status)
  }

  private def withActorSystem[T](block: Materializer => T): T = {
    implicit val system: ActorSystem        = ActorSystem("web-socket-flow-handler-spec")
    implicit val materializer: Materializer = Materializer.matFromSystem
    try {
      block(materializer)
    } finally {
      Await.result(system.terminate(), 5.seconds)
    }
  }
}
