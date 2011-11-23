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
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder

import org.jboss.netty.channel.group._
import java.util.concurrent._

import play.core._
import play.core.server.websocket._
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import org.apache.commons.codec.binary.Base64

import java.nio.charset.Charset
import java.security.MessageDigest

trait Handshake {

  val ASCII = Charset.forName("ASCII");
  val SHA_1 = MessageDigest.getInstance("SHA1")
  val ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

  def support(req: HttpRequest): Boolean

  def upgrade(req: HttpRequest, res: HttpResponse, ctx: ChannelHandlerContext)

  protected def getWebSocketLocation(request: HttpRequest) = "ws://" + request.getHeader(HttpHeaders.Names.HOST) + request.getUri()

  protected def adjustPipelineToHixie(ctx: ChannelHandlerContext) {
    val p = ctx.getChannel().getPipeline();
    p.remove("aggregator");
    p.replace("decoder", "wsdecoder", new WebSocket00FrameDecoder());
    p.replace("encoder", "wsencoder", new WebSocket00FrameEncoder());
  }

  protected def adjustPipelineToHybi(ctx: ChannelHandlerContext) {
    val p = ctx.getChannel().getPipeline();
    p.remove("aggregator");
    p.replace("decoder", "wsdecoder", new WebSocket10FrameDecoder());
    p.replace("encoder", "wsencoder", new WebSocket10FrameEncoder());
  }

  protected def sha1(s: String) = {
    SHA_1.digest(s.getBytes(ASCII))
  }

}

object WebSocketHandshake {

  val protocols = List(Hixie75, Hixie76, Hybi06, Hybi07, Hybi10, Hybi13)

  def shake(ctx: ChannelHandlerContext, req: HttpRequest): HttpResponse = {

    // Create the WebSocket handshake response.
    val res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(101, "Web Socket Protocol Handshake"))
    res.addHeader(HttpHeaders.Names.UPGRADE, HttpHeaders.Values.WEBSOCKET)
    res.addHeader(CONNECTION, HttpHeaders.Values.UPGRADE)

    // Check which protocol we support
    protocols map (proto => {
      if (proto.support(req)) {
        proto.upgrade(req, res, ctx)
      }
    })

    res
  }

}

object Hixie75 extends Handshake {

  def support(request: HttpRequest): Boolean = {
    (request.containsHeader(CONNECTION) && request.getHeader(CONNECTION).equals(HttpHeaders.Values.UPGRADE) && !request.containsHeader(SEC_WEBSOCKET_KEY1) && !request.containsHeader("Sec-WebSocket-Version"))
  }

  def upgrade(req: HttpRequest, res: HttpResponse, ctx: ChannelHandlerContext) {
    res.setStatus(new HttpResponseStatus(101, "Web Socket Protocol Handshake"));
    res.addHeader(HttpHeaders.Values.UPGRADE, "websocket");
    res.addHeader(CONNECTION, HttpHeaders.Values.UPGRADE);
    res.addHeader(WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
    res.addHeader(WEBSOCKET_LOCATION, getWebSocketLocation(req));
    val protocol = req.getHeader(WEBSOCKET_PROTOCOL);
    if (protocol != null) {
      res.addHeader(WEBSOCKET_PROTOCOL, protocol);
    }
    ctx.getChannel().write(res)
    adjustPipelineToHixie(ctx)
  }
}

object Hixie76 extends Handshake {

  def support(request: HttpRequest): Boolean = {
    (request.containsHeader(SEC_WEBSOCKET_KEY1) && request.containsHeader(SEC_WEBSOCKET_KEY2))
  }

  def upgrade(req: HttpRequest, res: HttpResponse, ctx: ChannelHandlerContext) {
    res.setStatus(new HttpResponseStatus(101, "Web Socket Protocol Handshake"));
    res.addHeader(HttpHeaders.Values.UPGRADE, "websocket");
    res.addHeader(CONNECTION, HttpHeaders.Values.UPGRADE);
    res.addHeader(SEC_WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
    res.addHeader(SEC_WEBSOCKET_LOCATION, getWebSocketLocation(req));
    val protocol = req.getHeader(SEC_WEBSOCKET_PROTOCOL);
    if (protocol != null) {
      res.addHeader(SEC_WEBSOCKET_PROTOCOL, protocol);
    }

    // Calculate the answer of the challenge.
    val key1 = req.getHeader(SEC_WEBSOCKET_KEY1);
    val key2 = req.getHeader(SEC_WEBSOCKET_KEY2);
    val a = (key1.replaceAll("[^0-9]", "").toLong / key1.replaceAll("[^ ]", "").length()).toInt
    val b = (key2.replaceAll("[^0-9]", "").toLong / key2.replaceAll("[^ ]", "").length()).toInt
    val c = req.getContent().readLong()
    val input = ChannelBuffers.buffer(16)
    input.writeInt(a)
    input.writeInt(b)
    input.writeLong(c)
    import java.security.NoSuchAlgorithmException

    try {
      import java.security.MessageDigest
      val output: ChannelBuffer = ChannelBuffers.wrappedBuffer(MessageDigest.getInstance("MD5").digest(input.array()))
      res.setContent(output)
    } catch { case ex: NoSuchAlgorithmException => throw new UnexpectedException(unexpected = Some(ex)) }
    ctx.getChannel().write(res)
    adjustPipelineToHixie(ctx)
  }
}

object Hybi06 extends Handshake {

  def support(request: HttpRequest): Boolean = {
    request.containsHeader("Sec-WebSocket-Version") && request.getHeader("Sec-WebSocket-Version").toInt == 6
  }

  def upgrade(req: HttpRequest, res: HttpResponse, ctx: ChannelHandlerContext) {
    // TODO: should work the same as hybi10, need to verify that
  }
}

object Hybi07 extends Handshake {

  def support(request: HttpRequest): Boolean = {
    request.containsHeader("Sec-WebSocket-Version") && request.getHeader("Sec-WebSocket-Version").toInt == 7
  }

  def upgrade(req: HttpRequest, res: HttpResponse, ctx: ChannelHandlerContext) {
    // TODO: should work the same as hybi10, need to verify that
  }
}

object Hybi10 extends Handshake {

  def support(request: HttpRequest): Boolean = {
    request.containsHeader("Sec-WebSocket-Version") && request.getHeader("Sec-WebSocket-Version").toInt == 8
  }

  def upgrade(req: HttpRequest, response: HttpResponse, ctx: ChannelHandlerContext) {
    val key = req.getHeader("Sec-WebSocket-Key")
    if (key == null) {
      response.setStatus(HttpResponseStatus.BAD_REQUEST)
    }

    val accept = new String(Base64.encodeBase64(sha1(key + ACCEPT_GUID)))

    response.setStatus(new HttpResponseStatus(101, "Switching Protocols"))
    response.addHeader(HttpHeaders.Values.UPGRADE, "websocket")
    response.addHeader(CONNECTION, HttpHeaders.Values.UPGRADE)
    response.addHeader("Sec-WebSocket-Accept", accept)
    ctx.getChannel().write(response)
    adjustPipelineToHybi(ctx)
  }
}

object Hybi13 extends Handshake {

  def support(request: HttpRequest): Boolean = {
    request.containsHeader("Sec-WebSocket-Version") && request.getHeader("Sec-WebSocket-Version").toInt == 13
  }

  def upgrade(req: HttpRequest, response: HttpResponse, ctx: ChannelHandlerContext) {
    // should work the same as hybi10, need to verify that
    val key = req.getHeader("Sec-WebSocket-Key")
    if (key == null) {
      response.setStatus(HttpResponseStatus.BAD_REQUEST)
    }

    val accept = new String(Base64.encodeBase64(sha1(key + ACCEPT_GUID)))

    response.setStatus(new HttpResponseStatus(101, "Switching Protocols"))
    response.addHeader(HttpHeaders.Values.UPGRADE, "websocket")
    response.addHeader(CONNECTION, HttpHeaders.Values.UPGRADE)
    response.addHeader("Sec-WebSocket-Accept", accept)
    ctx.getChannel().write(response)
    adjustPipelineToHybi(ctx)
  }
}

