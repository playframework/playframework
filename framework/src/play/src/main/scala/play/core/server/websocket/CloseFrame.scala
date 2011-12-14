package play.core.server.websocket

/**
 * Websocket close frame.
 */
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.buffer.ChannelBuffers
import java.nio.charset.Charset
import java.util._
/**
 * Websocket close frame.
 */
class CloseFrame(rsv: Int) extends Frame(true, rsv, ChannelBuffers.EMPTY_BUFFER) {

}