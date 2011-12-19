package play.core.server.websocket

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import java.nio.charset.Charset
import java.util._
/**
 * Websocket binary frame.
 */
case class BinaryFrame(override val finalFragment: Boolean, override val rsv: Int, override val binaryData: ChannelBuffer) extends Frame(finalFragment, rsv, binaryData)