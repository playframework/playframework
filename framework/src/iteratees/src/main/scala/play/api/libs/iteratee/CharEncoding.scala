/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.iteratee

import java.io.{ ByteArrayOutputStream, StringWriter }
import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.charset._
import play.api.libs.iteratee.Execution.defaultExecutionContext
import scala.annotation.tailrec

/**
 * [[Enumeratee]]s for converting chunks of bytes to chunks of chars, and vice-versa.
 *
 * These methods can handle cases where characters straddle a chunk boundary, and redistribute the data.
 * An erroneous encoding or an incompatible decoding causes a [[Step.Error]].
 *
 * @define javadoc http://docs.oracle.com/javase/8/docs/api
 */
object CharEncoding {

  private trait Coder[From, To] extends Enumeratee[From, To] {
    private type Inner[A] = Iteratee[To, A]

    protected def empty: From

    protected def code(data: From, last: Boolean): Either[CoderResult, (To, From)]

    protected def concat(a: From, b: From): From

    private def step[A](initial: From = empty)(it: Inner[A]): K[From, Inner[A]] = {
      case in @ Input.El(chars) =>
        it.pureFlatFold[From, Inner[A]] {
          case Step.Cont(k) =>
            code(concat(initial, chars), false).fold({ result =>
              Error(s"coding error: $result", in)
            }, {
              case (bytes, remaining) =>
                val newIt = Iteratee.flatten(it.feed(Input.El(bytes)))
                Cont(step(remaining)(newIt))
            })
          case _ => Done(it)
        }(defaultExecutionContext)
      case in @ Input.Empty =>
        val newIt = Iteratee.flatten(it.feed(in))
        Cont(step(initial)(newIt))
      case in @ Input.EOF =>
        code(initial, true).fold({ result =>
          Error(s"coding error: $result", in)
        }, {
          case (string, remaining) =>
            val newIt = Iteratee.flatten(it.feed(Input.El(string)).flatMap(_.feed(in))(defaultExecutionContext))
            Done(newIt)
        })
    }

    def applyOn[A](inner: Inner[A]) = Cont(step()(inner))
  }

  def decode(charset: Charset): Enumeratee[Array[Byte], String] = new Coder[Array[Byte], String] {
    protected val empty = Array[Byte]()

    protected def concat(a: Array[Byte], b: Array[Byte]) = a ++ b

    protected def code(bytes: Array[Byte], last: Boolean) = {
      val decoder = charset.newDecoder

      val byteBuffer = ByteBuffer.wrap(bytes)
      // at least 2, for UTF-32
      val charBuffer = CharBuffer.allocate(2 max math.ceil(bytes.length * decoder.averageCharsPerByte).toInt)
      val out = new StringWriter

      @tailrec
      def process(charBuffer: CharBuffer): CoderResult = {
        val result = decoder.decode(byteBuffer, charBuffer, true)
        out.write(charBuffer.array, 0, charBuffer.position)
        if (result.isOverflow) {
          if (charBuffer.position == 0) {
            // shouldn't happen for most encodings
            process(CharBuffer.allocate(2 * charBuffer.capacity))
          } else {
            charBuffer.clear()
            process(charBuffer)
          }
        } else {
          result
        }
      }
      val result = process(charBuffer)

      if (result.isUnmappable || last && result.isMalformed) {
        Left(result)
      } else {
        val remaining = if (result.isError) bytes.drop(byteBuffer.position) else empty
        Right((out.toString, remaining))
      }
    }
  }

  /**
   * @throws scala.Exception [[$javadoc/java/nio/charset/UnsupportedCharsetException.html UnsupportedCharsetException]] if no
   *                  charset could be found with the provided name
   */
  def decode(charset: String): Enumeratee[Array[Byte], String] = decode(Charset.forName(charset))

  def encode(charset: Charset): Enumeratee[String, Array[Byte]] = new Coder[String, Array[Byte]] {
    protected def empty = ""

    protected def concat(a: String, b: String) = a + b

    protected def code(chars: String, last: Boolean) = {
      val encoder = charset.newEncoder

      val charBuffer = CharBuffer.wrap(chars)
      // at least 6, for UTF-8
      val byteBuffer = ByteBuffer.allocate(6 max math.ceil(chars.length * encoder.averageBytesPerChar).toInt)
      val out = new ByteArrayOutputStream
      @tailrec
      def process(byteBuffer: ByteBuffer): CoderResult = {
        val result = encoder.encode(charBuffer, byteBuffer, true)
        out.write(byteBuffer.array, 0, byteBuffer.position)
        if (result.isOverflow) {
          if (byteBuffer.position == 0) {
            // shouldn't happen for most encodings
            process(ByteBuffer.allocate(2 * byteBuffer.capacity))
          } else {
            byteBuffer.clear()
            process(byteBuffer)
          }
        } else {
          result
        }
      }
      val result = process(byteBuffer)
      if (result.isUnmappable || last && result.isMalformed) {
        Left(result)
      } else {
        val remaining = if (result.isError) chars.drop(charBuffer.position) else ""
        val bytes = out.toByteArray
        val bytesWithoutBom = if (charset.name.startsWith("UTF-") && bytes.length >= 2 && bytes(0) == 0xfe.toByte && bytes(1) == 0xff.toByte) {
          bytes.drop(2)
        } else {
          bytes
        }
        Right((bytesWithoutBom, remaining))
      }
    }

  }

  /**
   * @throws scala.Exception [[$javadoc/java/nio/charset/UnsupportedCharsetException.html UnsupportedCharsetException]] if no
   *                  charset could be found with the provided name
   */
  def encode(charset: String): Enumeratee[String, Array[Byte]] = encode(Charset.forName(charset))

}
