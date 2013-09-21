package play.core.server.websocket

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.buffer._
import org.jboss.netty.channel._
import org.jboss.netty.bootstrap._
import org.jboss.netty.channel.Channels._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.channel.socket.nio._
import org.jboss.netty.handler.stream._
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpHeaders.Values._
import org.jboss.netty.handler.codec.http.websocketx._

import org.jboss.netty.channel.group._
import java.util.concurrent._

import play.core._
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import org.apache.commons.codec.binary.Base64

import java.nio.charset.Charset
import java.security.MessageDigest

object WebSocketHandshake {
  protected def getWebSocketLocation(request: HttpRequest) = "ws://" + request.getHeader(HttpHeaders.Names.HOST) + request.getUri()

  def shake(ctx: ChannelHandlerContext, req: HttpRequest): Unit = {
    val factory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req),
      "*", /* wildcard to accept all subprotocols */
      true /* allowExtensions */ )

    val shaker = factory.newHandshaker(req)

    // HACK ALERT: the netty websocket handshaker wants to remove
    // an HttpChunkAggregator and throws an exception when it
    // isn't in the pipeline. We just put it in here so it can be
    // taken back out, as a workaround. Needs better fix.
    val pipeline = ctx.getChannel().getPipeline()
    pipeline.addLast("hack-remove-this-chunk-aggregator", new HttpChunkAggregator(Int.MaxValue));

    shaker.handshake(ctx.getChannel(), req)

    // be sure the HttpChunkAggregator goes away, if handshake
    // didn't remove it as expected.
    try {
      pipeline.remove(classOf[HttpChunkAggregator])
    } catch {
      case ex: NoSuchElementException =>
      // this is what we're expecting, since handshake removed it
    }
  }

}
