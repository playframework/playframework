package play.core.server.websocket

import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame
import org.jboss.netty.handler.codec.replay.ReplayingDecoder
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.channel._
import java.util._
import play.api._

/**
 * This code is largely inspired from the Netty project but adapted to the Netty 3.6 version
 */
class WebSocket10FrameDecoder extends ReplayingDecoder[DecodingState](DecodingState.FRAME_START) {

  val OPCODE_CONT = 0x0
  val OPCODE_TEXT = 0x1
  val OPCODE_BINARY = 0x2
  val OPCODE_CLOSE = 0x8
  val OPCODE_PING = 0x9
  val OPCODE_PONG = 0xA

  // TODO: Continuation frame
  //var frames: Seq[ChannelBuffer]

  var fragmentOpcode: Option[Int] = None
  var opcode: Option[Int] = None
  var currentFrameLength = -1
  var maskingKey: Option[ChannelBuffer] = None
  var reserved: Option[Int] = None
  var finale = true

  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer, state: DecodingState): Object = {

    state match {

      case DecodingState.FRAME_START => {
        val b = buffer.readByte
        val fin = (b & 0x80).toByte
        finale = (fin == 0)
        reserved = Some(b & 0x70)
        opcode = Some(b & 0x0F)

        if (reserved.get != 0) {
          throw new Exception("Reserved bits set: " + bits(reserved.get))
        }
        if (!isOpcode(opcode.get)) {
          throw new Exception("Invalid opcode " + hex(opcode.get))
        }
        if (fin == 0) {
          fragmentOpcode match {
            case None => {
              if (!isDataOpcode(opcode.get)) {
                throw new Exception("Fragmented frame with invalid opcode " + hex(opcode.get))
              }
              fragmentOpcode = opcode
            }
            case Some(a) => {
              throw new Exception("Continuation frame with invalid opcode " + hex(opcode.get))
            }
          }
        } else {
          fragmentOpcode match {
            case Some(a) => {
              if (!isControlOpcode(opcode.get) && opcode.get != OPCODE_CONT) {
                throw new Exception("Final frame with invalid opcode " + hex(opcode.get))
              }
            }
            case None => {
              if (opcode.get == OPCODE_CONT) {
                throw new Exception("Final frame with invalid opcode " + hex(opcode.get))
              }
              this.opcode = opcode
            }
          }
        }

        checkpoint(DecodingState.PARSING_LENGTH)
        return null
      }
      case DecodingState.PARSING_LENGTH => {
        val b = buffer.readByte
        val masked = b & 0x80
        if (masked == 0) {
          throw new Exception("Unmasked frame received")
        }
        val length = b & 0x7F
        if (length < 126) {
          currentFrameLength = length
          checkpoint(DecodingState.MASKING_KEY)
        } else if (length == 126) {
          checkpoint(DecodingState.PARSING_LENGTH_2)
        } else if (length == 127) {
          checkpoint(DecodingState.PARSING_LENGTH_3)
        }
        return null
      }
      case DecodingState.PARSING_LENGTH_2 => {
        val s = buffer.readUnsignedShort
        currentFrameLength = s;
        checkpoint(DecodingState.MASKING_KEY)
        return null
      }
      case DecodingState.PARSING_LENGTH_3 => {
        currentFrameLength = buffer.readLong.toInt
        checkpoint(DecodingState.MASKING_KEY)
        return null
      }
      case DecodingState.MASKING_KEY => {
        maskingKey = Some(buffer.readBytes(4))
        checkpoint(DecodingState.PAYLOAD)
        return null
      }
      case DecodingState.PAYLOAD => {
        val frame = unmask(buffer.readBytes(currentFrameLength))
        println("frame length " + frame.readableBytes)
        // TODO: Continuation
        // if (this.opcode.get == OPCODE_CONT) {
        // 		 	this.opcode = fragmentOpcode;
        // 			frames.add(frame);
        // 			frame = channel.getConfig().getBufferFactory().getBuffer(0);
        // 			for (ChannelBuffer channelBuffer : frames) {
        // 				                          frame.ensureWritableBytes(channelBuffer.readableBytes());
        // 				                          frame.writeBytes(channelBuffer);
        //  			}
        // 				  
        // 			this.fragmentOpcode = null;
        //           	frames.clear();
        //             checkpoint(DecodingState.FRAME_START);
        //             return null;
        //         }
        try {
          opcode.get match {
            case OPCODE_TEXT => return new TextFrame(finale, reserved.get, frame)
            case OPCODE_BINARY => return new BinaryFrame(finale, reserved.get, frame)
            case OPCODE_PING => return new PongFrame(reserved.get)
            case OPCODE_PONG => return null
            case OPCODE_CLOSE => return new CloseFrame(reserved.get)
          }
          return null
        } finally {
          checkpoint(DecodingState.FRAME_START);
        }
      }
    }

  }

  def isOpcode(opcode: Int) = {
    opcode == OPCODE_CONT ||
      opcode == OPCODE_TEXT ||
      opcode == OPCODE_BINARY ||
      opcode == OPCODE_CLOSE ||
      opcode == OPCODE_PING ||
      opcode == OPCODE_PONG
  }

  def bits(b: Int) = {
    Integer.toBinaryString(b).substring(24)
  }

  def hex(b: Int) = {
    Integer.toHexString(b)
  }

  def isControlOpcode(opcode: Int) = {
    opcode == OPCODE_CLOSE ||
      opcode == OPCODE_PING ||
      opcode == OPCODE_PONG
  }

  def isDataOpcode(opcode: Int) = {
    opcode == OPCODE_TEXT ||
      opcode == OPCODE_BINARY
  }

  def unmask(frame: ChannelBuffer) = {
    val bytes = frame.array()
    for (i <- 0 until bytes.length) {
      val b = (frame.getByte(i) ^ maskingKey.get.getByte(i % 4)).toByte
      frame.setByte(i, b);
    }
    frame
  }

}