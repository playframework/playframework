/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
*/
package play.api.mvc

import akka.stream.scaladsl.Flow
import play.api.http.websocket.Message

object WebSocketFlowResponse {

  implicit def flowToWebsocketFlow(flow: Flow[Message, Message, _]): WebSocketFlowResponse = WebSocketFlowResponse(flow, None)

}

case class WebSocketFlowResponse(flow: Flow[Message, Message, _], subprotocol: Option[String])

