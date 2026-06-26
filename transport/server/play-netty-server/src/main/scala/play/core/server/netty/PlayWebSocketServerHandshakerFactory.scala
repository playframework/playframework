/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import io.netty.handler.codec.http.websocketx._
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpRequest

private[netty] final class PlayWebSocketServerHandshakerFactory(
    webSocketURL: String,
    subprotocols: String,
    decoderConfig: WebSocketDecoderConfig,
    extraHeaders: Seq[(String, String)]
) extends WebSocketServerHandshakerFactory(webSocketURL, subprotocols, decoderConfig) {

  override def newHandshaker(req: HttpRequest): WebSocketServerHandshaker = {
    Option(req.headers().get(HttpHeaderNames.SEC_WEBSOCKET_VERSION)) match {
      case Some(version) if version.toString == WebSocketVersion.V13.toHttpHeaderValue =>
        new Handshaker13(webSocketURL, subprotocols, decoderConfig)
      case Some(version) if version.toString == WebSocketVersion.V08.toHttpHeaderValue =>
        new Handshaker08(webSocketURL, subprotocols, decoderConfig)
      case Some(version) if version.toString == WebSocketVersion.V07.toHttpHeaderValue =>
        new Handshaker07(webSocketURL, subprotocols, decoderConfig)
      case Some(_) =>
        null
      case None =>
        new Handshaker00(webSocketURL, subprotocols, decoderConfig)
    }
  }

  private def merge(responseHeaders: HttpHeaders): HttpHeaders = {
    if (extraHeaders.isEmpty) {
      responseHeaders
    } else {
      val merged = Option(responseHeaders).getOrElse(new DefaultHttpHeaders())
      extraHeaders.foreach { case (name, value) => merged.add(name, value) }
      merged
    }
  }

  private final class Handshaker13(
      webSocketURL: String,
      subprotocols: String,
      decoderConfig: WebSocketDecoderConfig
  ) extends WebSocketServerHandshaker13(webSocketURL, subprotocols, decoderConfig) {
    protected override def newHandshakeResponse(req: FullHttpRequest, responseHeaders: HttpHeaders): FullHttpResponse =
      super.newHandshakeResponse(req, merge(responseHeaders))
  }

  private final class Handshaker08(
      webSocketURL: String,
      subprotocols: String,
      decoderConfig: WebSocketDecoderConfig
  ) extends WebSocketServerHandshaker08(webSocketURL, subprotocols, decoderConfig) {
    protected override def newHandshakeResponse(req: FullHttpRequest, responseHeaders: HttpHeaders): FullHttpResponse =
      super.newHandshakeResponse(req, merge(responseHeaders))
  }

  private final class Handshaker07(
      webSocketURL: String,
      subprotocols: String,
      decoderConfig: WebSocketDecoderConfig
  ) extends WebSocketServerHandshaker07(webSocketURL, subprotocols, decoderConfig) {
    protected override def newHandshakeResponse(req: FullHttpRequest, responseHeaders: HttpHeaders): FullHttpResponse =
      super.newHandshakeResponse(req, merge(responseHeaders))
  }

  private final class Handshaker00(
      webSocketURL: String,
      subprotocols: String,
      decoderConfig: WebSocketDecoderConfig
  ) extends WebSocketServerHandshaker00(webSocketURL, subprotocols, decoderConfig) {
    protected override def newHandshakeResponse(req: FullHttpRequest, responseHeaders: HttpHeaders): FullHttpResponse =
      super.newHandshakeResponse(req, merge(responseHeaders))
  }
}
