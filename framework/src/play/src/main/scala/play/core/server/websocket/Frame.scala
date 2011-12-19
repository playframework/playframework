package play.core.server.websocket

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import java.nio.charset.Charset
import org.jboss.netty.util.CharsetUtil

class Frame(val finalFragment: Boolean, val rsv: Int, val binaryData: ChannelBuffer) {
  def getTextData() = { binaryData.toString(CharsetUtil.UTF_8) }
}

case class FrameFormatter[A](toFrame: A => Frame, fromFrame: PartialFunction[Frame, A]) extends play.api.mvc.WebSocket.FrameFormatter[A] {

  def transform[B](fba: B => A, fab: A => B): FrameFormatter[B] = {
    FrameFormatter[B](
      toFrame.compose(fba),
      fromFrame.andThen(fab))
  }

}

object Frames {

  val textFrame = FrameFormatter[String](
    str => TextFrame(true, 0, str),
    { case TextFrame(_, _, str) => str })

  val binaryFrame = FrameFormatter[Array[Byte]](
    bytes => BinaryFrame(true, 0, org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer(bytes)),
    { case BinaryFrame(_, _, b) => b.array })

}

