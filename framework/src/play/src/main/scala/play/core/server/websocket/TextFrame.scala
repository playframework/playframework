package play.core.server.websocket

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import java.util._
/**
 * Websocket text frame.
 */
case class TextFrame(override val finalFragment: Boolean, override val rsv: Int, text: String) extends Frame(finalFragment, rsv, ChannelBuffers.copiedBuffer(text, CharsetUtil.UTF_8)) {

  def this(finalFragment: Boolean, rsv: Int, binaryData: ChannelBuffer) = { this(finalFragment, rsv, binaryData.toString(CharsetUtil.UTF_8)) }

}