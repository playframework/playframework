package play.core.server.netty

import org.jboss.netty.channel.ChannelFuture
import play.api.libs.concurrent._
import scala.concurrent.{ ExecutionContext, CanAwait}
import scala.concurrent.util.Duration
import java.util.concurrent.TimeUnit
import org.jboss.netty.channel.ChannelFutureListener

/**
 * provides a play.api.libs.concurrent.Promise implementation based on Netty's
 * ChannelFuture
 */
object NettyPromise {

  def apply(channelPromise: ChannelFuture) = new scala.concurrent.Future[Unit] {
    parent =>

    def isCompleted: Boolean = channelPromise.isDone

    def onComplete[U](func: (Either[Throwable, Unit]) ⇒ U)(implicit executor: ExecutionContext): Unit = channelPromise.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          val r = if (future.isSuccess()) Right(()) else Left(future.getCause())
          executor.execute(new Runnable() { def run() { func(r) } })
        }
      })

    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
      if(channelPromise.await(atMost.toMillis))
        this
      else throw new scala.concurrent.TimeoutException("netty channel future await timeout after: "+atMost)
    }

    def result(atMost: Duration)(implicit permit: CanAwait): Unit = {
      val done = (channelPromise.await(atMost.toMillis))
      (done, channelPromise.isSuccess) match {
        case (false,_) => throw new scala.concurrent.TimeoutException("netty channel future await timeout after: "+atMost)
        case (true,false) => throw channelPromise.getCause
        case (true,true) => ()

      }
    }

    def value: Option[Either[Throwable, Unit]] = (channelPromise.isDone,channelPromise.isSuccess) match {
      case (true,true) => Some(Right(()))
      case (true,false) => Some(Left(channelPromise.getCause))
      case _ => None
    }
  }
}
