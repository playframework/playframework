package play.core.server.netty

import org.jboss.netty.channel.ChannelFuture
import play.api.libs.concurrent._
import java.util.concurrent.TimeUnit
import org.jboss.netty.channel.ChannelFutureListener

/**
 * provides a play.api.libs.concurrent.Promise implementation based on Netty's
 * ChannelFuture
 */
object NettyPromise {

  def apply(channelPromise: ChannelFuture) = new play.api.libs.concurrent.Promise[Unit] {
    parent =>

    def onRedeem(k: Unit => Unit) {
      channelPromise.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          if (future.isSuccess()) k()
        }
      })
    }

    def extend[B](k: Function1[Promise[Unit], B]): Promise[B] = {
      val p = Promise[B]()
      channelPromise.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          p.redeem(k(parent))
        }
      })
      p
    }

    def await(timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): NotWaiting[Unit] = {

      if (channelPromise.await(timeout, unit))
        if (channelPromise.isSuccess()) Redeemed(())
        else Thrown(channelPromise.getCause())
      else {
        throw new java.util.concurrent.TimeoutException("Promise timed out after " + timeout + " : " + unit)

      }
    }

    def filter(predicate: Unit => Boolean): Promise[Unit] = {
      val p = Promise[Unit]()
      channelPromise.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          if (future.isSuccess()) {
            if (predicate()) p.redeem(Unit)
          } else p.throwing(future.getCause())

        }
      })
      p
    }

    def map[B](f: Unit => B): Promise[B] = {
      val p = Promise[B]()
      channelPromise.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          if (future.isSuccess()) {
            p.redeem(f())
          } else p.throwing(future.getCause())

        }
      })
      p
    }

    def flatMap[B](f: Unit => Promise[B]): Promise[B] = {
      val p = Promise[B]()
      channelPromise.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          if (future.isSuccess()) {
            f().extend1 {
              case Redeemed(b) => p.redeem(b)
              case Thrown(e) => p.throwing(e)
            }
          } else p.throwing(future.getCause())
        }
      })
      p
    }

  }

}
