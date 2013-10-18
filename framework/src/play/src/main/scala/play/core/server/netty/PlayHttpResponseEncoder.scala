/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.buffer.ChannelBuffer

import play.core.utils.{ InstrumentationHelpers, WithInstrumentation }
import play.instrumentation.spi.PlayInstrumentation

class PlayHttpResponseEncoder extends HttpResponseEncoder with WithInstrumentation {
  import InstrumentationHelpers._

  private final def recordOutputBytes(cb: ChannelBuffer)(implicit instrumentation: PlayInstrumentation): Unit = {
    val l = cb.readableBytes().toLong
    if (l > 0) {
      recordOutputBodyBytes(l)
    }
  }

  override def handleDownstream(ctx: ChannelHandlerContext, evt: ChannelEvent) {
    super.handleDownstream(ctx, evt)
  }

  override protected def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    Option(channel.getAttachment.asInstanceOf[ChannelAttachment]) match {
      case Some(attachment) =>
        implicit val instrumentation = attachment.instrumentation
        var r = msg match {
          case m: HttpChunk =>
            recordOutputChunk(toPlayChunk(m))
            Option(m.getContent).foreach(recordOutputBytes)
            super.encode(ctx, channel, msg)
          case m: HttpResponse =>
            // Because we record the result type here
            // It appears we begin processing output before
            // we know what kind of result we have.
            recordOutputHeader(toPlayOutputHeader(m))
            val length = HttpHeaders.getContentLength(m)
            if (!m.isChunked) {
              recordSimpleResult(m.getStatus.getCode)
              recordExpectedOutputBodyBytes(length.intValue)
            } else {
              recordChunkedResult(m.getStatus.getCode)
            }
            Option(m.getContent).foreach(recordOutputBytes)
            super.encode(ctx, channel, msg)
          case m: ChannelBuffer => {
            recordOutputBytes(m)
            super.encode(ctx, channel, msg)
          }
          case m @ _ => {
            super.encode(ctx, channel, msg)
          }
        }
        r
      case None => super.encode(ctx, channel, msg)
    }
  }
}
