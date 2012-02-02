package play.core.server.websocket

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import java.nio.charset.Charset
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame

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
    { case frame: TextWebSocketFrame => frame.getText })

  val binaryFrame = FrameFormatter[Array[Byte]](
    bytes => new BinaryWebSocketFrame(true, 0, org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer(bytes)),
    { case frame: BinaryWebSocketFrame => frame.getBinaryData().array() })

  val mixedFrame = FrameFormatter[Either[String, Array[Byte]]](
    stringOrBytes => {
      stringOrBytes.fold(
        str => new TextWebSocketFrame(true, 0, str),
        bytes => new BinaryWebSocketFrame(true, 0, org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer(bytes))
      )
    },
    {
      case frame: TextWebSocketFrame => Left(frame.getText)
      case frame: BinaryWebSocketFrame => Right(frame.getBinaryData.array)
    }
  )

}

