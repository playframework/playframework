/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import org.jboss.netty.channel.{ MessageEvent, ChannelHandlerContext }

case class NettyBackendRequest(context: ChannelHandlerContext, event: MessageEvent)
