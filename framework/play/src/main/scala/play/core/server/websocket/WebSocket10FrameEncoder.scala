package play.core.server.websocket

import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame
import org.jboss.netty.handler.codec.replay.ReplayingDecoder
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelHandler.Sharable
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.oneone._
import java.util._
import play.api._
import org.jboss.netty.handler.codec.http.websocket._
import java.nio._


/**
 * This code is largely inspired from the Netty project but adapted to the Netty 3.6 version
 */
@Sharable
class WebSocket10FrameEncoder extends OneToOneEncoder {

  val OPCODE_CONT = 0x0;
  val OPCODE_TEXT = 0x1;
  val OPCODE_BINARY = 0x2;
  val OPCODE_CLOSE = 0x8;
  val OPCODE_PING = 0x9;
  val OPCODE_PONG = 0xA;

  val maskPayload = true;

  @Override
  def encode(ctx: ChannelHandlerContext, channel: Channel, msg: Object): Object = {
    msg match {
      case frame: Frame =>
        {
          val data: ChannelBuffer = if (frame.binaryData == null) ChannelBuffers.EMPTY_BUFFER else frame.binaryData
          val opcode: Byte = frame match {
            case x: TextFrame => OPCODE_TEXT.toByte
            case x: PingFrame => OPCODE_PING.toByte
            case x: PongFrame => OPCODE_PONG.toByte
            case x: CloseFrame => OPCODE_PONG.toByte
            case x: BinaryFrame => OPCODE_BINARY.toByte
            case x: ContinuationFrame => OPCODE_CONT.toByte
            case _ => throw new Exception("Cannot encode frame of type: " + frame.getClass().getName())
          }

          val length = data.readableBytes
          var b0 = 0
          if (frame.finalFragment) {
            b0 |= (1 << 7)
          }
          b0 |= ((frame.rsv % 8) << 4)
          b0 |= (opcode % 128)

          if (opcode == OPCODE_PING && length > 125) {
            throw new Exception("invalid payload for PING (payload length must be <= 125, was " + length)
          }

          val maskLength = if (maskPayload) 4 else 0
          val header: ChannelBuffer = maskLength match {
            case maskLength if length <= 125 => {
              val header = ChannelBuffers.buffer(2 + maskLength)
              header.writeByte(b0)
              val b = if (maskPayload) (0x80 | length) else length
              header.writeByte(b)
              header
            }
            case maskLength if length <= 0xFFFF => {
              val header = ChannelBuffers.buffer(4 + maskLength)
              header.writeByte(b0)
              header.writeByte(if (maskPayload) (0xFE) else 126)
              header.writeByte((length >>> 8) & 0xFF)
              header.writeByte((length) & 0xFF)
              header
            }
            case _ => {
              val header = ChannelBuffers.buffer(10 + maskLength)
              header.writeByte(b0)
              header.writeByte(if (maskPayload) (0xFF) else 127)
              header.writeLong(length)
              header
            }
          }

          val body = if (maskPayload) {
            val body = ChannelBuffers.buffer(length)
            val random = (Math.random * Integer.MAX_VALUE).toInt
            val mask = ByteBuffer.allocate(4).putInt(random).array

            header.writeBytes(mask)

            for (counter <- 0 until data.readableBytes) {
              val byteData = data.readByte
              val x = (counter % 4) // +counter++
              body.writeByte(byteData ^ mask(counter.unary_+ % 4))
            }
            body
          } else {
            data
          }
          return ChannelBuffers.wrappedBuffer(header, body);
        }
    }
    return msg;
  }
}
