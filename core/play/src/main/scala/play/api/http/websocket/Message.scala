/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http.websocket

import org.apache.pekko.util.ByteString

/**
 * A WebSocket message.
 *
 * This is a high level API intended for common simple use cases.  It allows handling and sending of full
 * WebSocket messages, as well as close and ping/pong messages.  It will buffer fragmented messages up until
 * a limit, and does not allow streaming in fragments.
 */
sealed trait Message

/**
 * A text message.
 *
 * @param data The data of the text message.
 */
case class TextMessage(data: String) extends Message

/**
 * A binary message.
 *
 * @param data The data of the binary message.
 */
case class BinaryMessage(data: ByteString) extends Message

/**
 * A close message.
 *
 * RFC 6455 reserves close status codes 1005, 1006, and 1015 for application-visible state. Applications should
 * not send those values as the status code in a WebSocket Close frame. When a status code is present, the close
 * reason must fit in the remaining 123 UTF-8 bytes of the Close frame payload. Play truncates longer close
 * reasons before sending them.
 *
 * @param statusCode The close status code.
 * @param reason The reason it was closed.
 */
case class CloseMessage(statusCode: Option[Int] = Some(CloseCodes.Regular), reason: String = "") extends Message

object CloseMessage {
  def apply(statusCode: Int): CloseMessage =
    CloseMessage(Some(statusCode), "")
  def apply(statusCode: Int, reason: String): CloseMessage =
    CloseMessage(Some(statusCode), reason)
}

/**
 * A ping message.
 *
 * @param data The application data.
 */
case class PingMessage(data: ByteString) extends Message

/**
 * A pong message.
 *
 * @param data The application data.
 */
case class PongMessage(data: ByteString) extends Message

/**
 * An exception that, if thrown by a WebSocket source, will cause the WebSocket to be closed with the given close
 * message. This is a convenience that allows the WebSocket to close with a particular close code without having
 * to produce generic Messages.
 */
case class WebSocketCloseException(message: CloseMessage) extends RuntimeException(message.reason, null, false, false)
