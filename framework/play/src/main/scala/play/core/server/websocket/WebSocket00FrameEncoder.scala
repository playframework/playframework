package play.core.server.websocket

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelHandler.Sharable
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder

/**
 * This code is largely inspired from the Netty project but adapted to the Netty 3.6 version
 */
@Sharable
class WebSocket00FrameEncoder extends OneToOneEncoder {

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: Object): Object = {
    msg match {
      case frame: TextFrame =>
        {
          // Text frame
          val data = frame.binaryData
          val encoded =
            channel.getConfig().getBufferFactory().getBuffer(
              data.order, data.readableBytes + 2)
          // type should given per the client but it is safe to use 0x00 (from the spec)
          encoded.writeByte(0x00)
          encoded.writeBytes(data, data.readerIndex, data.readableBytes)
          encoded.writeByte(0xFF)
          return encoded
        }
      /**
       * From the spec: !!!  WARNING: At this time, the WebSocket protocol cannot be used to
       * send binary data.  Using any of the frame types other than 0x00 and
       * 0xFF is invalid.  All other frame types are reserved for future use
       * by future versions of this protocol.
       */
      case frame: BinaryFrame =>
        {
          // Binary frame
          val data = frame.binaryData
          val dataLen = data.readableBytes
          val encoded =
            channel.getConfig().getBufferFactory().getBuffer(
              data.order(), dataLen + 5);

          // Encode type.
          encoded.writeByte(0x80);

          // Encode length.
          val b1 = dataLen >>> 28 & 0x7F
          val b2 = dataLen >>> 14 & 0x7F
          val b3 = dataLen >>> 7 & 0x7F
          val b4 = dataLen & 0x7F
          if (b1 == 0) {
            if (b2 == 0) {
              if (b3 == 0) {
                encoded.writeByte(b4)
              } else {
                encoded.writeByte(b3 | 0x80)
                encoded.writeByte(b4)
              }
            } else {
              encoded.writeByte(b2 | 0x80)
              encoded.writeByte(b3 | 0x80)
              encoded.writeByte(b4)
            }
          } else {
            encoded.writeByte(b1 | 0x80)
            encoded.writeByte(b2 | 0x80)
            encoded.writeByte(b3 | 0x80)
            encoded.writeByte(b4)
          }

          // Encode binary data.
          encoded.writeBytes(data, data.readerIndex, dataLen)
          return encoded;
        }
    }
    return msg;
  }
}

