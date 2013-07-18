package play.core.server.netty

import org.jboss.netty.channel.ChannelFuture
import play.api.libs.concurrent._
import scala.concurrent.{ ExecutionContext, CanAwait }
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import org.jboss.netty.channel.ChannelFutureListener
import scala.util._

/**
 * provides a play.api.libs.concurrent.Promise implementation based on Netty's
 * ChannelFuture
 */
object NettyPromise {

  def apply(channelPromise: ChannelFuture) = {

    val p = scala.concurrent.Promise[Unit]()
    channelPromise.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (future.isSuccess()) p.success(()) else p.failure(future.getCause())
      }
    })
    p.future
  }
}
