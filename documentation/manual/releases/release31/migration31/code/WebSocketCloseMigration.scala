/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scaladoc.websocketmigration

import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import play.api.http.websocket.CloseCodes
import play.api.http.websocket.CloseMessage
import play.api.http.websocket.Message
import play.api.http.websocket.WebSocketCloseException
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc.WebSocket

object WebSocketCloseMigration {
  def recordAbnormalClosure(): Unit = ()

  def abnormalClosureWebSocket: WebSocket = {
    // #abnormal-closure
    // Before: an abrupt transport close could complete the stream without a close message.
    // After: raw Message handlers receive CloseMessage(Some(1006), ...).
    WebSocket.accept[Message, Message] { _ =>
      Flow.fromSinkAndSource(
        Sink.foreach {
          case CloseMessage(Some(CloseCodes.ConnectionAbort), _) => recordAbnormalClosure()
          case _                                                 => ()
        },
        Source.maybe[Message]
      )
    }
    // #abnormal-closure
  }

  def closeExceptionWebSocket: WebSocket = {
    // #websocket-close-exception
    // Before: the embedded close status was not preserved reliably.
    // After: Play closes the WebSocket with status 4001.
    WebSocket.accept[Message, Message] { _ =>
      Flow.fromSinkAndSource(
        Sink.ignore,
        Source.failed(WebSocketCloseException(CloseMessage(4001, "Application close")))
      )
    }
    // #websocket-close-exception
  }

  object TypedJsonWebSocket {
    case class In(name: String)
    implicit val reads: Reads[In]                                           = Json.reads[In]
    implicit val transformer: WebSocket.MessageFlowTransformer[In, JsValue] =
      WebSocket.MessageFlowTransformer.jsonMessageFlowTransformer[In, JsValue]

    def webSocket: WebSocket = {
      // #typed-json-validation
      // Before: {"unknown":"value"} could fail to close with the intended 1003 status.
      // After: Play closes the WebSocket with status 1003 and the validation errors as reason.
      WebSocket.accept[In, JsValue] { _ =>
        Flow.fromSinkAndSource(Sink.ignore, Source.maybe[JsValue])
      }
      // #typed-json-validation
    }
  }
}
