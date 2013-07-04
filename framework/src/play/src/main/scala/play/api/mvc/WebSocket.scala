package play.api.mvc

import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scala.concurrent.Future

import play.core.Execution.Implicits.internalContext

/**
 * A WebSocket handler.
 *
 * @tparam A the socket messages type
 * @param f the socket messages generator
 */
case class WebSocket[A](f: RequestHeader => (Enumerator[A], Iteratee[A, Unit]) => Unit)(implicit val frameFormatter: WebSocket.FrameFormatter[A]) extends Handler {

  type FRAMES_TYPE = A

  /**
   * Returns itself, for better support in the routes file.
   *
   * @return itself
   */
  def apply() = this

}

/**
 * Helper utilities to generate WebSocket results.
 */
object WebSocket {

  /**
   * Typeclass to handle WebSocket frames format.
   */
  trait FrameFormatter[A] {

    /**
     * Transform a FrameFormatter[A] to a FrameFormatter[B]
     */
    def transform[B](fba: B => A, fab: A => B): FrameFormatter[B]

  }

  /**
   * Defaults frame formatters.
   */
  object FrameFormatter {

    /**
     * String WebSocket frames.
     */
    implicit val stringFrame: FrameFormatter[String] = play.core.server.websocket.Frames.textFrame

    /**
     * Array[Byte] WebSocket frames.
     */
    implicit val byteArrayFrame: FrameFormatter[Array[Byte]] = play.core.server.websocket.Frames.binaryFrame

    /**
     * Either String or Array[Byte] WebSocket frames.
     */
    implicit val mixedFrame: FrameFormatter[Either[String, Array[Byte]]] = play.core.server.websocket.Frames.mixedFrame

    /**
     * Json WebSocket frames.
     */
    implicit val jsonFrame: FrameFormatter[JsValue] = stringFrame.transform(Json.stringify, Json.parse)

  }

  /**
   * Creates a WebSocket result from inbound and outbound channels.
   */
  def using[A](f: RequestHeader => (Iteratee[A, _], Enumerator[A]))(implicit frameFormatter: FrameFormatter[A]): WebSocket[A] = {
    WebSocket[A](h => (e, i) => { val (readIn, writeOut) = f(h); e |>> readIn; writeOut |>> i })
  }

  def adapter[A](f: RequestHeader => Enumeratee[A, A])(implicit frameFormatter: FrameFormatter[A]): WebSocket[A] = {
    WebSocket[A](h => (in, out) => { in &> f(h) |>> out })
  }

  /**
   * Creates a WebSocket result from inbound and outbound channels retrieved asynchronously.
   */
  def async[A](f: RequestHeader => Future[(Iteratee[A, _], Enumerator[A])])(implicit frameFormatter: FrameFormatter[A]): WebSocket[A] = {
    using { rh =>
      val p = f(rh)
      val it = Iteratee.flatten(p.map(_._1))
      val enum = Enumerator.flatten(p.map(_._2))
      (it, enum)
    }
  }

}
