package play.api.mvc

import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

/**
 * A WebSocket handler.
 *
 * @tparam A the socket messages type
 * @param f the socket messages generator
 */
case class WebSocket[A](f: RequestHeader => (Enumerator[A], Iteratee[A, Unit]) => Unit)(implicit val frameFormatter: WebSocket.FrameFormatter[A]) extends Handler

/** Helper utilities to generate WebSocket results. */
object WebSocket {

  trait FrameFormatter[A] {

    def transform[B](fba: B => A, fab: A => B): FrameFormatter[B]

  }

  object FrameFormatter {

    implicit val stringFrame: FrameFormatter[String] = play.core.server.websocket.Frames.textFrame
    implicit val byteArrayFrame: FrameFormatter[Array[Byte]] = play.core.server.websocket.Frames.binaryFrame
    implicit val jsonFrame: FrameFormatter[JsValue] = stringFrame.transform(Json.stringify, Json.parse)

  }

  /**
   * Creates a WebSocket result from inbound and outbound channels.
   *
   * @param readIn the inboud channel
   * @param writeOut the outbound channel
   * @return a `WebSocket`
   */
  def using[A](f: RequestHeader => (Iteratee[A, Unit], Enumerator[A]))(implicit frameFormatter: FrameFormatter[A]) = {
    WebSocket[A](h => (e, i) => { val (readIn, writeOut) = f(h); e |>> readIn; writeOut |>> i })
  }

}
