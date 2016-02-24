/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.formatters

import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets._
import java.util.concurrent.ThreadLocalRandom

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Source }
import akka.stream.stage.{ Context, PushPullStage, SyncDirective, TerminationDirective }
import akka.util.{ ByteString, ByteStringBuilder }
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.Part

import scala.annotation.tailrec

object Multipart {
  private[this] def CrLf = "\r\n"

  private[this] val alphabet = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(US_ASCII)

  /**
   * Transforms a `Source[MultipartFormData.Part]` to a `Source[ByteString]`
   */
  def transform(body: Source[MultipartFormData.Part[Source[ByteString, _]], _], boundary: String): Source[ByteString, _] = {
    body.via(format(boundary, Charset.defaultCharset(), 4096))
  }

  /**
   * Provides a Formatting Flow which could be used to format a MultipartFormData.Part source to a multipart/form data body
   */
  def format(boundary: String, nioCharset: Charset, chunkSize: Int): Flow[MultipartFormData.Part[Source[ByteString, _]], ByteString, NotUsed] = {
    Flow[MultipartFormData.Part[Source[ByteString, _]]].transform(() => streamed(boundary, nioCharset, chunkSize))
      .flatMapConcat(identity)
  }

  /**
   * Creates a new random number of the given length and base64 encodes it (using a custom "safe" alphabet).
   *
   * @throws IllegalArgumentException if the length is greater than 70 or less than 1 as specified in
   *                                  <a href="https://tools.ietf.org/html/rfc2046#section-5.1.1">rfc2046</a>
   */
  def randomBoundary(length: Int = 18, random: java.util.Random = ThreadLocalRandom.current()): String = {
    if (length < 1 && length > 70) throw new IllegalArgumentException("length can't be greater than 70 or less than 1")
    val bytes: Seq[Byte] = for (byte <- 1 to length) yield {
      alphabet(random.nextInt(alphabet.length))
    }
    new String(bytes.toArray, US_ASCII)
  }

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
    private[this] val builder = new ByteStringBuilder
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
        val bytes = new Array[Byte](byteBuffer.remaining())
        byteBuffer.get(bytes)
        builder.putBytes(bytes)
      }
      charBuffer.clear()
    }

  }

  private class ByteStringFormatter(sizeHint: Int) extends Formatter {
    private[this] val builder = new ByteStringBuilder
    builder.sizeHint(sizeHint)

    def get: ByteString = builder.result

    def ~~(char: Char): this.type = {
      builder += char.toByte
      this
    }

  }

  private def streamed(boundary: String,
    nioCharset: Charset,
    chunkSize: Int): PushPullStage[MultipartFormData.Part[Source[ByteString, _]], Source[ByteString, Any]] =
    new PushPullStage[MultipartFormData.Part[Source[ByteString, _]], Source[ByteString, Any]] {
      var firstBoundaryRendered = false

      override def onPush(bodyPart: Part[Source[ByteString, _]], ctx: Context[Source[ByteString, Any]]): SyncDirective = {
        val f = new CustomCharsetByteStringFormatter(nioCharset, chunkSize)

        def bodyPartChunks(data: Source[ByteString, Any]): Source[ByteString, Any] = {
          (Source.single(f.get) ++ data).mapMaterializedValue((_) => ())
        }

        def completePartFormatting(): Source[ByteString, Any] = bodyPart match {
          case MultipartFormData.DataPart(_, data) => Source.single((f ~~ ByteString(data)).get)
          case MultipartFormData.FilePart(_, _, _, ref) => bodyPartChunks(ref)
          case _ => throw new UnsupportedOperationException()
        }

        renderBoundary(f, boundary, suppressInitialCrLf = !firstBoundaryRendered)
        firstBoundaryRendered = true

        val (key, filename, contentType) = bodyPart match {
          case MultipartFormData.DataPart(innerKey, _) => (innerKey, None, Option("text/plain"))
          case MultipartFormData.FilePart(innerKey, innerFilename, innerContentType, _) => (innerKey, Option(innerFilename), innerContentType)
          case _ => throw new UnsupportedOperationException()
        }
        renderDisposition(f, key, filename)
        contentType.foreach { ct => renderContentType(f, ct) }
        renderBuffer(f)
        ctx.push(completePartFormatting())
      }

      override def onPull(ctx: Context[Source[ByteString, Any]]): SyncDirective = {
        val finishing = ctx.isFinishing
        if (finishing && firstBoundaryRendered) {
          val f = new ByteStringFormatter(boundary.length + 4)
          renderFinalBoundary(f, boundary)
          ctx.pushAndFinish(Source.single(f.get))
        } else if (finishing) {
          ctx.finish()
        } else {
          ctx.pull()
        }
      }

      override def onUpstreamFinish(ctx: Context[Source[ByteString, Any]]): TerminationDirective = ctx.absorbTermination()

    }

  private def renderBoundary(f: Formatter, boundary: String, suppressInitialCrLf: Boolean = false): Unit = {
    if (!suppressInitialCrLf) f ~~ CrLf
    f ~~ '-' ~~ '-' ~~ boundary ~~ CrLf
  }

  private def renderFinalBoundary(f: Formatter, boundary: String): Unit =
    f ~~ CrLf ~~ '-' ~~ '-' ~~ boundary ~~ '-' ~~ '-'

  private def renderDisposition(f: Formatter, contentDisposition: String, filename: Option[String]): Unit = {
    f ~~ "Content-Disposition: form-data; name=" ~~ '"' ~~ contentDisposition ~~ '"'
    filename.foreach { name => f ~~ "; filename=" ~~ '"' ~~ name ~~ '"' }
    f ~~ CrLf
  }

  private def renderContentType(f: Formatter, contentType: String): Unit = {
    f ~~ "Content-Type: " ~~ contentType ~~ CrLf
  }

  private def renderBuffer(f: Formatter): Unit = {
    f ~~ CrLf
  }

}