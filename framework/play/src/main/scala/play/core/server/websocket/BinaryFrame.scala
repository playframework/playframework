package play.core.server.websocket

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import java.nio.charset.Charset
import java.util._
/**
 * Websocket binary frame.
 */
class BinaryFrame(finalFragment: Boolean, rsv: Int, binaryData: ChannelBuffer) extends Frame(finalFragment, rsv, binaryData) {

}