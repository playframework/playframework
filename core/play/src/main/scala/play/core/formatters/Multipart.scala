/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.formatters

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets._
import java.nio.CharBuffer
import java.util.concurrent.ThreadLocalRandom

import scala.annotation.tailrec

import org.apache.pekko.stream._
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.stage._
import org.apache.pekko.util.ByteString
import org.apache.pekko.util.ByteStringBuilder
import org.apache.pekko.NotUsed
import play.api.mvc.MultipartFormData

object Multipart {
  private[this] def CrLf = "\r\n"

  private[this] val alphabet = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(US_ASCII)

  /**
   * Transforms a `Source[MultipartFormData.Part]` to a `Source[ByteString]`
   */
  def transform(
      body: Source[MultipartFormData.Part[Source[ByteString, ?]], ?],
      boundary: String
  ): Source[ByteString, ?] = {
    body.via(format(boundary, Charset.defaultCharset(), 4096))
  }

  /**
   * Provides a Formatting Flow which could be used to format a MultipartFormData.Part source to a multipart/form data body
   */
  def format(
      boundary: String,
      nioCharset: Charset,
      chunkSize: Int
  ): Flow[MultipartFormData.Part[Source[ByteString, ?]], ByteString, NotUsed] = {
    Flow[MultipartFormData.Part[Source[ByteString, ?]]]
      .via(streamed(boundary, nioCharset, chunkSize))
      .flatMapConcat(identity)
  }

  /**
   * Creates a new random number of the given length and base64 encodes it (using a custom "safe" alphabet).
   *
   * @throws java.lang.IllegalArgumentException if the length is greater than 70 or less than 1 as specified in
   *                                  <a href="https://tools.ietf.org/html/rfc2046#section-5.1.1">rfc2046</a>
   */
  def randomBoundary(length: Int = 18, random: java.util.Random = ThreadLocalRandom.current()): String = {
    if (length < 1 || length > 70) throw new IllegalArgumentException("length can't be greater than 70 or less than 1")
    val bytes: Seq[Byte] = for (byte <- 1 to length) yield {
      alphabet(random.nextInt(alphabet.length))
    }
    new String(bytes.toArray, US_ASCII)
  }

  /**
   * Helper function to escape a single header parameter using the HTML5 strategy.
   * (The alternative would be the strategy defined by RFC5987)
   * Particularly useful for Content-Disposition header parameters which might contain
   * non-ASCII values, like file names.
   * This follows the "WHATWG HTML living standard" section 4.10.21.8 and matches
   * the behavior of curl and modern browsers.
   * See https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#multipart-form-data
   */
  def escapeParamWithHTML5Strategy(value: String) =
    value
      .replace("\"", "%22")
      .replace("\r", "%0D")
      .replace("\n", "%0A")

  private sealed trait Formatter {
    def ~~(ch: Char): this.type

    def ~~(string: String): this.type = {
      @tailrec def rec(ix: Int = 0): this.type =
        if (ix < string.length) {
          this ~~ string.charAt(ix)
          rec(ix + 1)
        } else this
      rec()
    }
  }

  private class CustomCharsetByteStringFormatter(nioCharset: Charset, sizeHint: Int) extends Formatter {
    private[this] val charBuffer = CharBuffer.allocate(64)
    private[this] val builder    = new ByteStringBuilder
    builder.sizeHint(sizeHint)

    def get: ByteString = {
      flushCharBuffer()
      builder.result()
    }

    def ~~(char: Char): this.type = {
      if (!charBuffer.hasRemaining) flushCharBuffer()
      charBuffer.put(char)
      this
    }

    def ~~(bytes: ByteString): this.type = {
      if (bytes.nonEmpty) {
        flushCharBuffer()
        builder ++= bytes
      }
      this
    }

    private def flushCharBuffer(): Unit = {
      charBuffer.flip()
      if (charBuffer.hasRemaining) {
        val byteBuffer = nioCharset.encode(charBuffer)
        val bytes      = new Array[Byte](byteBuffer.remaining())
        byteBuffer.get(bytes)
        builder.putBytes(bytes)
      }
      charBuffer.clear()
    }
  }

  private class ByteStringFormatter(sizeHint: Int) extends Formatter {
    private[this] val builder = new ByteStringBuilder
    builder.sizeHint(sizeHint)

    def get: ByteString = builder.result()

    def ~~(char: Char): this.type = {
      builder += char.toByte
      this
    }
  }

  private def streamed(
      boundary: String,
      nioCharset: Charset,
      chunkSize: Int
  ): GraphStage[FlowShape[MultipartFormData.Part[Source[ByteString, ?]], Source[ByteString, Any]]] =
    new GraphStage[FlowShape[MultipartFormData.Part[Source[ByteString, ?]], Source[ByteString, Any]]] {
      val in  = Inlet[MultipartFormData.Part[Source[ByteString, ?]]]("CustomCharsetByteStringFormatter.in")
      val out = Outlet[Source[ByteString, Any]]("CustomCharsetByteStringFormatter.out")

      override def shape = FlowShape.of(in, out)

      override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
        new GraphStageLogic(shape) with OutHandler with InHandler {
          var firstBoundaryRendered = false

          override def onPush(): Unit = {
            val f = new CustomCharsetByteStringFormatter(nioCharset, chunkSize)

            val bodyPart = grab(in)

            def bodyPartChunks(data: Source[ByteString, Any]): Source[ByteString, Any] = {
              (Source.single(f.get) ++ data).mapMaterializedValue(_ => ())
            }

            def completePartFormatting(): Source[ByteString, Any] = bodyPart match {
              case MultipartFormData.DataPart(_, data)               => Source.single((f ~~ ByteString(data)).get)
              case MultipartFormData.FilePart(_, _, _, ref, _, _, _) => bodyPartChunks(ref)
              case _                                                 => throw new UnsupportedOperationException()
            }

            renderBoundary(f, boundary, suppressInitialCrLf = !firstBoundaryRendered)
            firstBoundaryRendered = true

            val (key, filename, contentType, dispositionType) = bodyPart match {
              case MultipartFormData.DataPart(innerKey, _) => (innerKey, None, Option("text/plain"), "form-data")
              case MultipartFormData.FilePart(
                    innerKey,
                    innerFilename,
                    innerContentType,
                    _,
                    _,
                    innerDispositionType,
                    _
                  ) =>
                (innerKey, Option(innerFilename), innerContentType, innerDispositionType)
              case _ => throw new UnsupportedOperationException()
            }
            renderDisposition(f, dispositionType, key, filename)
            contentType.foreach { ct => renderContentType(f, ct) }
            renderBuffer(f)
            push(out, completePartFormatting())
          }

          override def onPull(): Unit = {
            val finishing = isClosed(in)
            if (finishing && firstBoundaryRendered) {
              val f = new ByteStringFormatter(boundary.length + 4)
              renderFinalBoundary(f, boundary)
              push(out, Source.single(f.get))
              completeStage()
            } else if (finishing) {
              completeStage()
            } else {
              pull(in)
            }
          }

          override def onUpstreamFinish(): Unit = {
            if (isAvailable(out)) onPull()
          }

          setHandlers(in, out, this)
        }
    }

  private def renderBoundary(f: Formatter, boundary: String, suppressInitialCrLf: Boolean = false): Unit = {
    if (!suppressInitialCrLf) f ~~ CrLf
    f ~~ '-' ~~ '-' ~~ boundary ~~ CrLf
  }

  private def renderFinalBoundary(f: Formatter, boundary: String): Unit =
    f ~~ CrLf ~~ '-' ~~ '-' ~~ boundary ~~ '-' ~~ '-'

  private def renderDisposition(
      f: Formatter,
      dispositionType: String,
      contentDisposition: String,
      filename: Option[String]
  ): Unit = {
    f ~~ "Content-Disposition: " ~~ dispositionType ~~ "; name=" ~~ '"' ~~ escapeParamWithHTML5Strategy(
      contentDisposition
    ) ~~ '"'
    filename.foreach { name => f ~~ "; filename=" ~~ '"' ~~ escapeParamWithHTML5Strategy(name) ~~ '"' }
    f ~~ CrLf
  }

  private def renderContentType(f: Formatter, contentType: String): Unit = {
    f ~~ "Content-Type: " ~~ contentType ~~ CrLf
  }

  private def renderBuffer(f: Formatter): Unit = {
    f ~~ CrLf
  }
}
