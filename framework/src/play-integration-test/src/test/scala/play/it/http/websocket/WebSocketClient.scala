/**
 * Some elements of this were copied from:
 *
 * https://gist.github.com/casualjim/1819496
 */
package play.it.http.websocket

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.handler.codec.http._
import collection.JavaConversions._
import websocketx._
import java.net.{InetSocketAddress, URI}
import org.jboss.netty.util.CharsetUtil
import scala.concurrent.{Promise, Future}
import play.api.libs.iteratee.Execution.Implicits.trampoline
import play.api.libs.iteratee._

/**
 * A basic WebSocketClient.  Basically wraps Netty's WebSocket support into something that's much easier to use and much
 * more Scala friendly.
 */
trait WebSocketClient {
  
  import WebSocketClient._
  
  /**
   * Connect to the given URI.
   * 
   * @return A future that will be redeemed when the connection is closed.
   */
  def connect(url: URI, version: WebSocketVersion = WebSocketVersion.V13)
             (onConnect: Handler): Future[Unit]

  /**
   * Shutdown the client and release all associated resources.
   */
  def shutdown()
}

object WebSocketClient {

  type Handler = (Enumerator[WebSocketFrame], Iteratee[WebSocketFrame, _]) => Unit

  def create(): WebSocketClient = new DefaultWebSocketClient

  def apply[T](block: WebSocketClient => T) = {
    val client = WebSocketClient.create()
    try {
      block(client)
    } finally {
      client.shutdown()
    }
  }

  private implicit class ToFuture(chf: ChannelFuture) {
    def toScala: Future[Channel] = {
      val promise = Promise[Channel]()
      chf.addListener(new ChannelFutureListener {
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
  
  private class DefaultWebSocketClient extends WebSocketClient {
    val bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newSingleThreadExecutor(),
      Executors.newSingleThreadExecutor(), 1, 1))

    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline = {
        val pipeline = Channels.pipeline()
        pipeline.addLast("decoder", new HttpResponseDecoder)
        pipeline.addLast("encoder", new HttpRequestEncoder)
        pipeline
      }
    })


    /**
     * Connect to the given URI
     */
    def connect(url: URI, version: WebSocketVersion)
               (onConnected: (Enumerator[WebSocketFrame], Iteratee[WebSocketFrame, _]) => Unit) = {
      
      val normalized = url.normalize()
      val tgt = if (normalized.getPath == null || normalized.getPath.trim().isEmpty) {
        new URI(normalized.getScheme, normalized.getAuthority,"/", normalized.getQuery, normalized.getFragment)
      } else normalized
      
      val disconnected = Promise[Unit]()

      bootstrap.connect(new InetSocketAddress(tgt.getHost, tgt.getPort)).toScala.map { channel =>
        val handshaker = new WebSocketClientHandshakerFactory().newHandshaker(tgt, version, null, false, Map.empty[String, String])
        channel.getPipeline.addLast("supervisor", new WebSocketSupervisor(disconnected, handshaker, onConnected))
        handshaker.handshake(channel)
      }.onFailure {
        case t => disconnected.tryFailure(t)
      }
      
      disconnected.future
    }

    def shutdown() = bootstrap.releaseExternalResources()
  }

  private class WebSocketSupervisor(disconnected: Promise[Unit], handshaker: WebSocketClientHandshaker,
                                 onConnected: Handler) extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      e.getMessage match {
        case resp: HttpResponse if handshaker.isHandshakeComplete =>
          throw new WebSocketException("Unexpected HttpResponse (status=" + resp.getStatus +
            ", content=" + resp.getContent.toString(CharsetUtil.UTF_8) + ")")
        case resp: HttpResponse =>
          handshaker.finishHandshake(ctx.getChannel, e.getMessage.asInstanceOf[HttpResponse])
          ctx.getPipeline.addLast("websocket", new WebSocketClientHandler(ctx.getChannel, onConnected, disconnected))
        case _: WebSocketFrame => ctx.sendUpstream(e)
        case _ => throw new WebSocketException("Unexpected event: " + e)
      }
    }

    override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      disconnected.trySuccess(())
      ctx.sendDownstream(e)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      disconnected.tryFailure(e.getCause)
      ctx.getChannel.close()
      ctx.sendDownstream(e)
    }
  }
  
  private class WebSocketClientHandler(out: Channel, onConnected: Handler,
                                       disconnected: Promise[Unit]) extends SimpleChannelUpstreamHandler {

    val (enumerator, in) = Concurrent.broadcast[WebSocketFrame]

    val iteratee: Iteratee[WebSocketFrame, _] = Cont {
      case Input.El(wsf) => Iteratee.flatten(out.write(wsf).toScala.map(_ => iteratee))
      case Input.EOF => Iteratee.flatten(out.close().toScala.map(_ => Done((), Input.EOF)))
      case Input.Empty => iteratee
    }

    onConnected(enumerator, iteratee)

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      e.getMessage match {
        case close: CloseWebSocketFrame =>
          in.push(close)
          in.end()
          ctx.getChannel.disconnect()
        case wsf: WebSocketFrame =>
          in.push(wsf)
        case _ => throw new WebSocketException("Unexpected event: " + e)
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      disconnected.tryFailure(e.getCause)
      in.end(e.getCause)
      ctx.getChannel.close()
    }

    override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) = {
      disconnected.trySuccess(())
      in.end()
    }
  }

  class WebSocketException(s: String, th: Throwable) extends java.io.IOException(s, th) {
    def this(s: String) = this(s, null)
  }

}