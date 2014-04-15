package play.core.server.websocket

import org.jboss.netty.handler.codec.http.websocketx.{ WebSocketFrame, TextWebSocketFrame, BinaryWebSocketFrame }
import play.api.libs.iteratee.Input.El
import org.jboss.netty.buffer.ChannelBuffer

case class FrameFormatter[A](toFrame: A => WebSocketFrame, fromFrame: PartialFunction[WebSocketFrame, A]) extends play.api.mvc.WebSocket.FrameFormatter[A] {

  def transform[B](fba: B => A, fab: A => B): FrameFormatter[B] = {
    FrameFormatter[B](
      toFrame.compose(fba),
      fromFrame.andThen(fab))
  }

}

object Frames {

  val textFrame = FrameFormatter[String](
    str => new TextWebSocketFrame(true, 0, str),
    {
      case frame: TextWebSocketFrame => frame.getText
    })

  val binaryFrame = FrameFormatter[Array[Byte]](
    bytes => new BinaryWebSocketFrame(true, 0, org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer(bytes)),
    {
      case frame: BinaryWebSocketFrame => channelBufferToArray(frame.getBinaryData)
    })

  val mixedFrame = FrameFormatter[Either[String, Array[Byte]]](
    stringOrBytes => {
      stringOrBytes.fold(
        str => new TextWebSocketFrame(true, 0, str),
        bytes => new BinaryWebSocketFrame(true, 0, org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer(bytes))
      )
    },
    {
      case frame: TextWebSocketFrame => Left(frame.getText)
      case frame: BinaryWebSocketFrame => Right(channelBufferToArray(frame.getBinaryData))
    }
  )

  private def channelBufferToArray(buffer: ChannelBuffer) = {
    if (buffer.readableBytes() == buffer.capacity()) {
      // Use entire backing array
      buffer.array()
    } else {
      // Copy relevant bytes only
      val bytes = new Array[Byte](buffer.readableBytes())
      buffer.readBytes(bytes)
      bytes
    }
  }
}