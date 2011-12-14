package play.core.server.websocket

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import java.nio.charset.Charset
import org.jboss.netty.util.CharsetUtil

case class Frame(finalFragment: Boolean, rsv: Int, binaryData: ChannelBuffer) {
  def getTextData() = { binaryData.toString(CharsetUtil.UTF_8) }

}

