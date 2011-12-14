package play.core.server.websocket

import org.jboss.netty.handler.codec.replay.ReplayingDecoder
import org.jboss.netty.handler.codec.replay.VoidEnum
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.frame.TooLongFrameException
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.channel._
import java.util._
import play.api._

/**
 * This code is largely inspired from the Netty project but adapted to the Netty 3.6 version
 */
class WebSocket00FrameDecoder extends ReplayingDecoder[VoidEnum] {

  var receivedClosingHandshake = false
  val DEFAULT_MAX_FRAME_SIZE = 16384;

  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer, state: VoidEnum): Object = {

    // Discard all data received if closing handshake was received before.
    if (receivedClosingHandshake) {
      buffer.skipBytes(actualReadableBytes())
      return null;
    }

    // Decode a frame otherwise.
    val t = buffer.readByte()
    if ((t & 0x80) == 0x80) {
      // If the MSB on type is set, decode the frame length
      return decodeBinaryFrame(t, buffer)
    } else {
      // Decode a 0xff terminated UTF-8 string
      return decodeTextFrame(buffer)
    }
  }

  private def decodeBinaryFrame(t: Int, buffer: ChannelBuffer): Frame = {
    // TODO: Scala this
    var frameSize = 0L
    var lengthFieldSize = 0
    var b = 0
    do {
      b = buffer.readByte()
      frameSize <<= 7
      frameSize |= b & 0x7f
      if (frameSize > DEFAULT_MAX_FRAME_SIZE) {
        throw new TooLongFrameException()
      }
      lengthFieldSize += 1
      if (lengthFieldSize > 8) {
        // Perhaps a malicious peer?
        throw new TooLongFrameException()
      }
    } while ((b & 0x80) == 0x80)

    if (t == 0xFF && frameSize == 0) {
      receivedClosingHandshake = true
    }

    return new BinaryFrame(true, 0, buffer.readBytes(frameSize.toInt))
  }

  private def decodeTextFrame(buffer: ChannelBuffer): Frame = {
    val ridx = buffer.readerIndex()
    val rbytes = actualReadableBytes()
    val delimPos = buffer.indexOf(ridx, ridx + rbytes, 0xFF.toByte)
    if (delimPos == -1) {
      // Frame delimiter (0xFF) not found
      if (rbytes > DEFAULT_MAX_FRAME_SIZE) {
        // Frame length exceeded the maximum
        throw new TooLongFrameException()
      } else {
        // Wait until more data is received
        return null;
      }
    }

    val frameSize = delimPos - ridx
    if (frameSize > DEFAULT_MAX_FRAME_SIZE) {
      throw new TooLongFrameException()
    }

    val binaryData = buffer.readBytes(frameSize)
    buffer.skipBytes(1);
    return new TextFrame(true, 0, binaryData)
  }

}