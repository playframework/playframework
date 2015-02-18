/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.websocket

import play.api.mvc.WebSocket.FrameFormatter

/**
 * A type of WebSocket frame.
 */
sealed trait BasicFrame

/**
 * A text WebSocket frame.
 */
final case class TextFrame(text: String) extends BasicFrame

/**
 * A binary WebSocket frame.
 */
final case class BinaryFrame(bytes: Array[Byte]) extends BasicFrame
