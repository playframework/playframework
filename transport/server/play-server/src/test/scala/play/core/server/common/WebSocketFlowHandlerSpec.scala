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
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.QueueOfferResult
import org.apache.pekko.util.ByteString
import org.apache.pekko.Done
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import play.api.http.websocket.CloseCodes
import play.api.http.websocket.CloseMessage
import play.api.http.websocket.Message
import play.api.http.websocket.PingMessage
import play.api.http.websocket.PongMessage
import play.api.http.websocket.TextMessage

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

    "not send 1006 for a protocol violation already handled by the backend" in withActorSystem {
      implicit materializer =>
        val messages =
          runFailedRemoteInput(
            new WebSocketFlowHandler.BackendHandledProtocolViolation(new RuntimeException("protocol violation"))
          )

        messages must beEmpty
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

    "parse an empty close frame as application-visible 1005" in {
      WebSocketFlowHandler.parseCloseMessage(ByteString.empty) must beEqualTo(CloseMessage(CloseCodes.NoStatus))
    }

    "preserve a remote close frame with a reserved status code for application visibility" in {
      WebSocketFlowHandler.parseCloseMessage(rawCloseData(CloseCodes.ConnectionAbort)) must beEqualTo(
        CloseMessage(CloseCodes.ConnectionAbort)
      )
    }

    "preserve a remote close frame with an invalid status code range for application visibility" in {
      WebSocketFlowHandler.parseCloseMessage(rawCloseData(999)) must beEqualTo(CloseMessage(999))
    }

    "send 1006 after remote input completes even when the application has no immediate demand" in withActorSystem {
      implicit materializer =>
        val messages = runCompletedRemoteInputWithoutInitialAppDemand()

        messages must contain(exactly(closeMessage(CloseCodes.ConnectionAbort)))
    }

    "send a reserved application close status code to the remote peer" in withActorSystem { implicit materializer =>
      val message = runAppClose(CloseMessage(CloseCodes.ConnectionAbort))

      message must beEqualTo(CloseMessage(CloseCodes.ConnectionAbort))
    }

    "truncate an oversized application close reason before sending it to the remote peer" in withActorSystem {
      implicit materializer =>
        val message = runAppClose(CloseMessage(CloseCodes.Regular, "x" * 124))

        message must beEqualTo(CloseMessage(CloseCodes.Regular, "x" * 123))
    }

    "truncate an oversized application close reason to a valid UTF-8 prefix" in withActorSystem {
      implicit materializer =>
        val message = runAppClose(CloseMessage(CloseCodes.Regular, ("x" * 122) + "\u20ac"))

        message must beEqualTo(CloseMessage(CloseCodes.Regular, "x" * 122))
    }

    "drop an application close reason when no close status code is sent" in withActorSystem { implicit materializer =>
      val message = runAppClose(CloseMessage(None, "reason"))

      message must beEqualTo(CloseMessage(None))
    }

    "complete when the peer acknowledges an application close" in withActorSystem { implicit materializer =>
      val (remoteIn, remoteOut) = runClosingWebSocket(5.seconds)

      Await.result(remoteOut.pull(), 5.seconds) must beSome(closeMessage(CloseCodes.Regular))
      offer(remoteIn, rawClose(CloseCodes.Regular))
      Await.result(remoteOut.pull(), 1.second) must beNone
    }

    "complete when the peer does not acknowledge an application close before the timeout" in withActorSystem {
      implicit materializer =>
        val (remoteIn, remoteOut) = runClosingWebSocket(100.millis)

        Await.result(remoteOut.pull(), 5.seconds) must beSome(closeMessage(CloseCodes.Regular))
        Await.result(remoteOut.pull(), 5.seconds) must beNone
        remoteIn.complete()
        ok
    }

    "start the close timeout only after emitting the close frame" in withActorSystem { implicit materializer =>
      val (remoteIn, remoteOut) = runClosingWebSocket(100.millis)

      Thread.sleep(250)
      Await.result(remoteOut.pull(), 5.seconds) must beSome(closeMessage(CloseCodes.Regular))
      Await.result(remoteOut.pull(), 5.seconds) must beNone
      remoteIn.complete()
      ok
    }

    "complete when the transport terminates while awaiting a close acknowledgement" in withActorSystem {
      implicit materializer =>
        val (remoteIn, remoteOut) = runClosingWebSocket(5.seconds)

        Await.result(remoteOut.pull(), 5.seconds) must beSome(closeMessage(CloseCodes.Regular))
        remoteIn.complete()
        Await.result(remoteOut.pull(), 1.second) must beNone
    }

    "not let client ping or pong frames extend the close timeout" in withActorSystem { implicit materializer =>
      val (remoteIn, remoteOut) = runClosingWebSocket(1.second)

      Await.result(remoteOut.pull(), 5.seconds) must beSome(closeMessage(CloseCodes.Regular))
      offer(remoteIn, rawMessage(WebSocketFlowHandler.MessageType.Ping, ByteString("ping"), isFinal = true))
      offer(remoteIn, rawMessage(WebSocketFlowHandler.MessageType.Pong, ByteString("pong"), isFinal = true))
      Await.result(remoteOut.pull(), 5.seconds) must beNone
      remoteIn.complete()
      ok
    }

    "not send periodic keep-alive frames after emitting a close frame" in withActorSystem { implicit materializer =>
      val (remoteIn, remoteOut) =
        runClosingWebSocket(1.second, wsKeepAliveMode = "ping", wsKeepAliveMaxIdle = 20.millis)

      Await.result(remoteOut.pull(), 5.seconds) must beSome(closeMessage(CloseCodes.Regular))
      Await.result(remoteOut.pull(), 5.seconds) must beNone
      remoteIn.complete()
      ok
    }

    "answer a peer ping while the application applies backpressure" in withActorSystem { implicit materializer =>
      val (remoteIn, remoteOut, appMessages, appOut) = runBackpressuredWebSocket()
      val payload                                    = ByteString("ping")

      offer(remoteIn, rawMessage(WebSocketFlowHandler.MessageType.Ping, payload, isFinal = true))

      Await.result(remoteOut.pull(), 1.second) must beSome(beEqualTo(PongMessage(payload)))
      Await.result(appMessages.pull(), 1.second) must beSome(beEqualTo(PingMessage(payload)))

      appOut.complete()
      Await.result(remoteOut.pull(), 1.second) must beSome(closeMessage(CloseCodes.Regular))
      offer(remoteIn, rawClose(CloseCodes.Regular))
      Await.result(remoteOut.pull(), 1.second) must beNone
    }

    "consume a peer pong while the application applies backpressure" in withActorSystem { implicit materializer =>
      val (remoteIn, remoteOut, appMessages, _) = runBackpressuredWebSocket()
      val payload                               = ByteString("pong")

      offer(remoteIn, rawMessage(WebSocketFlowHandler.MessageType.Pong, payload, isFinal = true))
      Await.result(remoteIn.offer(rawClose(CloseCodes.Regular)), 1.second) must_== QueueOfferResult.Enqueued

      Await.result(appMessages.pull(), 1.second) must beSome(beEqualTo(PongMessage(payload)))
      val appClose = appMessages.pull()
      Await.result(remoteOut.pull(), 1.second) must beSome(closeMessage(CloseCodes.Regular))
      Await.result(appClose, 1.second) must beSome(closeMessage(CloseCodes.Regular))
      Await.result(remoteOut.pull(), 1.second) must beNone
      Await.result(appMessages.pull(), 1.second) must beNone
    }

    "acknowledge a peer close while the application applies backpressure" in withActorSystem { implicit materializer =>
      val (remoteIn, remoteOut, appMessages, _) = runBackpressuredWebSocket()

      offer(remoteIn, rawClose(CloseCodes.GoingAway))

      Await.result(remoteOut.pull(), 1.second) must beSome(closeMessage(CloseCodes.GoingAway))
      Await.result(remoteOut.pull(), 1.second) must beNone
      Await.result(appMessages.pull(), 1.second) must beSome(closeMessage(CloseCodes.GoingAway))
      Await.result(appMessages.pull(), 1.second) must beNone
    }

    "preserve a buffered application message before reporting abnormal closure" in withActorSystem {
      implicit materializer =>
        val (remoteIn, remoteOut, appMessages, _) = runBackpressuredWebSocket()

        offer(remoteIn, rawMessage(WebSocketFlowHandler.MessageType.Text, ByteString("message"), isFinal = true))
        remoteIn.complete()

        Await.result(appMessages.pull(), 1.second) must beSome(beEqualTo(TextMessage("message")))
        Await.result(appMessages.pull(), 1.second) must beSome(closeMessage(CloseCodes.ConnectionAbort))
        Await.result(appMessages.pull(), 1.second) must beNone
        Await.result(remoteOut.pull(), 1.second) must beNone
    }

    "preserve a buffered application message before a backend-handled protocol failure" in withActorSystem {
      implicit materializer =>
        val (remoteIn, remoteOut, appMessages, _) = runBackpressuredWebSocket()

        offer(remoteIn, rawMessage(WebSocketFlowHandler.MessageType.Text, ByteString("message"), isFinal = true))
        remoteIn.fail(new WebSocketFlowHandler.BackendHandledProtocolViolation(new RuntimeException("invalid frame")))

        Await.result(appMessages.pull(), 1.second) must beSome(beEqualTo(TextMessage("message")))
        Await.result(appMessages.pull(), 1.second) must beNone
        Await.result(remoteOut.pull(), 1.second) must beNone
    }

    "send 1001 only to the peer and complete graceful shutdown after its acknowledgement" in withActorSystem {
      implicit materializer =>
        val gracefulShutdown                   = new WebSocketGracefulShutdown
        val (remoteIn, remoteOut, appMessages) = runGracefullyClosingWebSocket(gracefulShutdown, 5.seconds)

        val shutdownComplete = gracefulShutdown.shutdown(CloseMessage(CloseCodes.GoingAway), 5.seconds)(
          materializer.system.scheduler,
          materializer.executionContext
        )
        Await.result(remoteOut.pull(), 1.second) must beSome(closeMessage(CloseCodes.GoingAway))
        shutdownComplete.isCompleted must beFalse

        offer(remoteIn, rawClose(CloseCodes.GoingAway))
        Await.result(remoteOut.pull(), 1.second) must beNone
        Await.result(appMessages.pull(), 1.second) must beNone
        Await.result(shutdownComplete, 1.second) must_== Done
    }

    "complete graceful shutdown when the close handshake times out" in withActorSystem { implicit materializer =>
      val gracefulShutdown         = new WebSocketGracefulShutdown
      val (remoteIn, remoteOut, _) = runGracefullyClosingWebSocket(gracefulShutdown, 100.millis)

      val shutdownComplete = gracefulShutdown.shutdown(CloseMessage(CloseCodes.GoingAway), 5.seconds)(
        materializer.system.scheduler,
        materializer.executionContext
      )
      Await.result(remoteOut.pull(), 1.second) must beSome(closeMessage(CloseCodes.GoingAway))
      Await.result(remoteOut.pull(), 1.second) must beNone
      Await.result(shutdownComplete, 1.second) must_== Done
      remoteIn.complete()
      ok
    }

    "wait for every WebSocket close handshake during graceful shutdown" in withActorSystem { implicit materializer =>
      val gracefulShutdown = new WebSocketGracefulShutdown
      val first            = runGracefullyClosingWebSocket(gracefulShutdown, 5.seconds)
      val second           = runGracefullyClosingWebSocket(gracefulShutdown, 5.seconds)

      val shutdownComplete = gracefulShutdown.shutdown(CloseMessage(CloseCodes.GoingAway), 5.seconds)(
        materializer.system.scheduler,
        materializer.executionContext
      )
      Await.result(first._2.pull(), 1.second) must beSome(closeMessage(CloseCodes.GoingAway))
      Await.result(second._2.pull(), 1.second) must beSome(closeMessage(CloseCodes.GoingAway))

      offer(first._1, rawClose(CloseCodes.GoingAway))
      Await.result(first._2.pull(), 1.second) must beNone
      shutdownComplete.isCompleted must beFalse

      offer(second._1, rawClose(CloseCodes.GoingAway))
      Await.result(second._2.pull(), 1.second) must beNone
      Await.result(shutdownComplete, 1.second) must_== Done
    }

    "bound graceful shutdown while the close output is backpressured" in withActorSystem { implicit materializer =>
      val gracefulShutdown         = new WebSocketGracefulShutdown
      val (remoteIn, remoteOut, _) = runGracefullyClosingWebSocket(gracefulShutdown, 5.seconds)

      val shutdownComplete = gracefulShutdown.shutdown(CloseMessage(CloseCodes.GoingAway), 100.millis)(
        materializer.system.scheduler,
        materializer.executionContext
      )
      shutdownComplete.isCompleted must beFalse
      Await.result(shutdownComplete, 1.second) must_== Done

      Await.result(remoteOut.pull(), 1.second) must beSome(closeMessage(CloseCodes.GoingAway))
      offer(remoteIn, rawClose(CloseCodes.GoingAway))
      Await.result(remoteOut.pull(), 1.second) must beNone
    }

    "echo an empty remote close frame without serializing 1005" in withActorSystem { implicit materializer =>
      val (_, remoteMessages) = runRemoteInputAndCollectAppAndRemoteMessages(rawClose(None))

      remoteMessages must contain(exactly(beEqualTo(CloseMessage(None))))
    }

    "reject reserved bits reported by a frame adapter" in withActorSystem { implicit materializer =>
      val (appMessages, remoteMessages) = runRemoteInputAndCollectAppAndRemoteMessages(
        rawMessage(
          WebSocketFlowHandler.MessageType.ReservedBits,
          ByteString.empty,
          isFinal = true
        ),
        rawClose(CloseCodes.ProtocolError)
      )

      appMessages must beEmpty
      remoteMessages must contain(exactly(closeMessage(CloseCodes.ProtocolError)))
    }

    Seq(
      WebSocketFlowHandler.MessageType.Ping,
      WebSocketFlowHandler.MessageType.Pong,
      WebSocketFlowHandler.MessageType.Close
    ).foreach { messageType =>
      s"reject a fragmented $messageType control frame" in withActorSystem { implicit materializer =>
        val (appMessages, remoteMessages) = runRemoteInputAndCollectAppAndRemoteMessages(
          rawMessage(messageType, ByteString("control"), isFinal = false),
          rawClose(CloseCodes.ProtocolError)
        )

        appMessages must beEmpty
        remoteMessages must contain(exactly(closeMessage(CloseCodes.ProtocolError)))
      }

      s"reject an oversized $messageType control frame" in withActorSystem { implicit materializer =>
        val (appMessages, remoteMessages) = runRemoteInputAndCollectAppAndRemoteMessages(
          rawMessage(messageType, ByteString(Array.fill[Byte](126)('a'.toByte)), isFinal = true),
          rawClose(CloseCodes.ProtocolError)
        )

        appMessages must beEmpty
        remoteMessages must contain(exactly(closeMessage(CloseCodes.ProtocolError)))
      }
    }

    "preserve fragmented data state while processing control frames" in withActorSystem { implicit materializer =>
      val (appMessages, remoteMessages) = runRemoteInputAndCollectAppAndRemoteMessages(
        rawMessage(WebSocketFlowHandler.MessageType.Text, ByteString("hel"), isFinal = false),
        rawMessage(WebSocketFlowHandler.MessageType.Ping, ByteString("ping"), isFinal = true),
        rawMessage(WebSocketFlowHandler.MessageType.Pong, ByteString("pong"), isFinal = true),
        rawMessage(WebSocketFlowHandler.MessageType.Continuation, ByteString("lo"), isFinal = true),
        rawClose(CloseCodes.Regular)
      )

      appMessages must beEqualTo(
        Seq(
          PingMessage(ByteString("ping")),
          PongMessage(ByteString("pong")),
          TextMessage("hello"),
          CloseMessage(CloseCodes.Regular)
        )
      )
      remoteMessages must beEqualTo(
        Seq(
          PongMessage(ByteString("ping")),
          CloseMessage(CloseCodes.Regular)
        )
      )
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
    runRemoteInputAndCollectAppAndRemoteMessages(remoteIn, appIn)._1
  }

  private def runRemoteInputAndCollectAppAndRemoteMessages(
      remoteIn: Source[WebSocketFlowHandler.RawMessage, ?],
      appIn: Source[Message, ?] = Source.maybe[Message]
  )(implicit materializer: Materializer): (Seq[Message], Seq[Message]) = {
    val appFlow = Flow.fromSinkAndSourceMat(Sink.seq[Message], appIn)(Keep.left)
    val flow    = protocol().joinMat(appFlow)(Keep.right)

    val (appMessages, remoteMessages) = remoteIn.viaMat(flow)(Keep.right).toMat(Sink.seq[Message])(Keep.both).run()

    (Await.result(appMessages, 5.seconds), Await.result(remoteMessages, 5.seconds))
  }

  private def runRemoteInputAndCollectAppAndRemoteMessages(
      messages: WebSocketFlowHandler.RawMessage*
  )(implicit materializer: Materializer): (Seq[Message], Seq[Message]) = {
    runRemoteInputAndCollectAppAndRemoteMessages(Source(messages.toList))
  }

  private def runAppClose(close: CloseMessage)(implicit materializer: Materializer): Message = {
    val appFlow = Flow.fromSinkAndSourceMat(
      Sink.seq[Message],
      Source.single(close)
    )(Keep.left)
    val flow = protocol().joinMat(appFlow)(Keep.right)

    val ((remoteIn, appMessages), remoteMessage) =
      Source
        .queue[WebSocketFlowHandler.RawMessage](1)
        .viaMat(flow)(Keep.both)
        .toMat(Sink.head[Message])(Keep.both)
        .run()

    val closeSent = Await.result(remoteMessage, 5.seconds)
    try {
      remoteIn.fail(new RuntimeException("connection failed"))
    } catch {
      case _: IllegalStateException =>
    }
    Await.result(appMessages, 5.seconds)
    closeSent
  }

  private def runAppInitiatedCloseThenFailedRemoteInput()(implicit materializer: Materializer): Seq[Message] = {
    val appFlow = Flow.fromSinkAndSourceMat(
      Sink.seq[Message],
      Source.single(CloseMessage(CloseCodes.Regular))
    )(Keep.left)
    val flow      = protocol().joinMat(appFlow)(Keep.right)
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

  private def runClosingWebSocket(
      closeTimeout: FiniteDuration,
      wsKeepAliveMode: String = "ping",
      wsKeepAliveMaxIdle: Duration = Duration.Inf
  )(implicit materializer: Materializer): (
      SourceQueueWithComplete[WebSocketFlowHandler.RawMessage],
      SinkQueueWithCancel[
        Message
      ]
  ) = {
    val appFlow = Flow.fromSinkAndSource(
      Sink.ignore,
      Source.single[Message](CloseMessage(CloseCodes.Regular))
    )
    val flow = protocol(closeTimeout, wsKeepAliveMode, wsKeepAliveMaxIdle).join(appFlow)

    Source
      .queue[WebSocketFlowHandler.RawMessage](16, OverflowStrategy.backpressure)
      .via(flow)
      .toMat(Sink.queue[Message]())(Keep.both)
      .run()
  }

  private def runBackpressuredWebSocket()(implicit materializer: Materializer): (
      SourceQueueWithComplete[WebSocketFlowHandler.RawMessage],
      SinkQueueWithCancel[Message],
      SinkQueueWithCancel[Message],
      SourceQueueWithComplete[Message]
  ) = {
    val appFlow = Flow.fromSinkAndSourceMat(
      Sink.queue[Message](),
      Source.queue[Message](1, OverflowStrategy.backpressure)
    )(Keep.both)
    val flow = protocol().joinMat(appFlow)(Keep.right)

    val ((remoteIn, (appMessages, appOut)), remoteOut) =
      Source
        .queue[WebSocketFlowHandler.RawMessage](1, OverflowStrategy.backpressure)
        .viaMat(flow)(Keep.both)
        .toMat(Sink.queue[Message]())(Keep.both)
        .run()

    (remoteIn, remoteOut, appMessages, appOut)
  }

  private def runGracefullyClosingWebSocket(
      gracefulShutdown: WebSocketGracefulShutdown,
      closeTimeout: FiniteDuration
  )(implicit materializer: Materializer): (
      SourceQueueWithComplete[WebSocketFlowHandler.RawMessage],
      SinkQueueWithCancel[Message],
      SinkQueueWithCancel[Message]
  ) = {
    val registered = Promise[Unit]()
    val register   = (close: CloseMessage => Unit) => {
      val unregister = gracefulShutdown.register(close)
      registered.success(())
      unregister
    }
    val appFlow =
      Flow.fromSinkAndSourceMat(Sink.queue[Message](), Source.maybe[Message])(Keep.left)
    val flow = protocol(closeTimeout, gracefulShutdown = Some(register)).joinMat(appFlow)(Keep.right)

    val ((remoteIn, appMessages), remoteOut) = Source
      .queue[WebSocketFlowHandler.RawMessage](1, OverflowStrategy.backpressure)
      .viaMat(flow)(Keep.both)
      .toMat(Sink.queue[Message]())(Keep.both)
      .run()
    Await.result(registered.future, 1.second)
    (remoteIn, remoteOut, appMessages)
  }

  private def offer(
      queue: SourceQueueWithComplete[WebSocketFlowHandler.RawMessage],
      message: WebSocketFlowHandler.RawMessage
  ): Unit = {
    Await.result(queue.offer(message), 5.seconds) must_== QueueOfferResult.Enqueued
  }

  private def runCompletedRemoteInputWithoutInitialAppDemand()(
      implicit materializer: Materializer
  ): Seq[Message] = {
    val appFlow = Flow.fromSinkAndSourceMat(Sink.asPublisher[Message](fanout = false), Source.maybe[Message])(Keep.left)
    val flow    = protocol().joinMat(appFlow)(Keep.right)

    val (appPublisher, remoteOutDone) =
      Source.empty[WebSocketFlowHandler.RawMessage].viaMat(flow)(Keep.right).toMat(Sink.ignore)(Keep.both).run()

    val appMessages = Source.fromPublisher(appPublisher).runWith(Sink.seq)

    awaitRemoteOut(remoteOutDone)
    Await.result(appMessages, 5.seconds)
  }

  private def protocol(
      closeTimeout: FiniteDuration = 3.seconds,
      wsKeepAliveMode: String = "ping",
      wsKeepAliveMaxIdle: Duration = Duration.Inf,
      gracefulShutdown: Option[(CloseMessage => Unit) => (() => Unit)] = None
  ) =
    WebSocketFlowHandler.webSocketProtocol(
      bufferLimit = 65536,
      wsKeepAliveMode = wsKeepAliveMode,
      wsKeepAliveMaxIdle = wsKeepAliveMaxIdle,
      wsCloseTimeout = closeTimeout,
      gracefulShutdown = gracefulShutdown
    )

  private def rawClose(statusCode: Int): WebSocketFlowHandler.RawMessage = {
    rawClose(Some(statusCode))
  }

  private def rawClose(statusCode: Option[Int]): WebSocketFlowHandler.RawMessage = {
    rawMessage(WebSocketFlowHandler.MessageType.Close, statusCode.fold(ByteString.empty)(rawCloseData), isFinal = true)
  }

  private def rawMessage(
      messageType: WebSocketFlowHandler.MessageType.Type,
      data: ByteString,
      isFinal: Boolean
  ): WebSocketFlowHandler.RawMessage = {
    WebSocketFlowHandler.RawMessage(messageType, data, isFinal)
  }

  private def rawCloseData(statusCode: Int): ByteString = {
    ByteString((statusCode >> 8).toByte, statusCode.toByte)
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
