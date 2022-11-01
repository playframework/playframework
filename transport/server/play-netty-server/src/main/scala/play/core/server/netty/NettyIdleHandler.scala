/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import io.netty.channel._
import io.netty.handler.timeout.IdleStateEvent
import play.api.Logger

private object NettyIdleHandler {
  private val logger: Logger = Logger(classOf[NettyIdleHandler])
}

private[play] class NettyIdleHandler extends ChannelInboundHandlerAdapter {
  import NettyIdleHandler._

  /**
   * Originally, this method lived within [[PlayRequestHandler]]. However, the [[PlayRequestHandler]]
   * gets removed from the Netty pipeline when a http connection gets upgraded to a websocket connection.
   * Therefore, for websockets, a user events would not be handled anymore, not calling this method and leaving
   * the connection open forever.
   */
  override def userEventTriggered(ctx: ChannelHandlerContext, evt: scala.Any): Unit = {
    evt match {
      case idle: IdleStateEvent if ctx.channel().isOpen =>
        logger.trace(s"Closing connection due to idle timeout")
        ctx.close()
      case _ => super.userEventTriggered(ctx, evt)
    }
  }
}
