/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.buffer.ChannelBuffer
import play.instrumentation.spi.{ PlayInstrumentationFactory, PlayInstrumentation }
import play.core.utils.WithInstrumentation
import scala.collection.JavaConversions._
import org.jboss.netty.handler.codec.http.HttpMessageDecoder.State

case class ChannelAttachment(finishedInitialRead: Boolean = false, instrumentation: PlayInstrumentation = null)
case class DeferredData(channel: Channel)

class PlayHttpRequestDecoder(instrumentationFactory: PlayInstrumentationFactory,
    maxInitialLineLength: Int,
    maxHeaderSize: Int,
    maxChunkSize: Int) extends HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize) with WithInstrumentation {

  var deferrefData: DeferredData = null

  protected override def createMessage(initialLine: Array[String]): HttpMessage = {
    val result: HttpRequest = super.createMessage(initialLine).asInstanceOf[HttpRequest]
    Option(deferrefData).map(_.channel.getAttachment.asInstanceOf[ChannelAttachment]).foreach { a =>
      implicit val instrumentation = a.instrumentation
      deferrefData.channel.setAttachment(a.copy(finishedInitialRead = true))
      recordRequestStart()
    }
    result
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    val channel = ctx.getChannel
    val attachment: ChannelAttachment = Option(channel.getAttachment.asInstanceOf[ChannelAttachment]).getOrElse {
      val a = ChannelAttachment(false, instrumentationFactory.createPlayInstrumentation())
      channel.setAttachment(a)
      a
    }
    if (!attachment.finishedInitialRead) {
      deferrefData = DeferredData(channel)
    }
    super.messageReceived(ctx, e)
    deferrefData = null
  }
}
