/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import org.jboss.netty.channel.{ Channel, ChannelFuture, ChannelFutureListener }
import scala.concurrent.{ Future, Promise }

/**
 * Allows a NettyFuture to be convert to a Scala Future
 */
object NettyFuture {

  implicit class ToScala(channelFuture: ChannelFuture) {

    def toScala: Future[Channel] = {
      val promise = Promise[Channel]()

      channelFuture.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) = {
          if (future.isSuccess) {
            promise.success(future.getChannel)
          } else if (future.isCancelled) {
            promise.failure(new RuntimeException("Future cancelled"))
          } else {
            promise.failure(future.getCause)
          }
        }
      })
      promise.future
    }
  }
}
