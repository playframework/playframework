package play.core.server.websocket

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import java.nio.charset.Charset
import java.util._

/**
 * Websocket continuation frame. This frame denotes a messages (binary data or text data) that is composed of multiple frames.
 */
class ContinuationFrame(finalFragment: Boolean, rsv: Int, binaryChannel: ChannelBuffer) extends Frame(finalFragment, rsv, binaryChannel) {

}