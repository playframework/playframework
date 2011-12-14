package play.api.mvc

import play.api.libs.iteratee._
import play.api.libs.concurrent._

/**
 * A WebSocket result.
 *
 * @tparam A the socket messages type
 * @param f the socket messages generator
 */
case class WebSocket[A](f: RequestHeader => (Enumerator[String], Iteratee[A, Unit]) => Unit)(implicit val writeable: Writeable[A]) extends Handler

/** Helper utilities to generate WebSocket results. */
object WebSocket {

  /**
   * Creates a WebSocket result from inbound and outbound channels.
   *
   * @param readIn the inboud channel
   * @param writeOut the outbound channel
   * @return a `WebSocket`
   */
  def using[A](f: RequestHeader => (Iteratee[String, Unit], Enumerator[A]))(implicit writeable: Writeable[A]) = {
    new WebSocket[A](h => (e, i) => { val (readIn, writeOut) = f(h); readIn <<: e; i <<: writeOut })
  }

}