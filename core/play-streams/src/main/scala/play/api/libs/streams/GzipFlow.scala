/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.streams

import java.util.zip.Deflater

import akka.stream.scaladsl.Compression
import akka.stream.scaladsl.Flow
import akka.stream.stage._
import akka.stream._
import akka.util.ByteString

/**
 * A simple Gzip Flow
 *
 * GZIPs each chunk separately.
 */
object GzipFlow {

  /**
   * Create a Gzip Flow with the given buffer size. The bufferSize controls how much data is sent to the Gzip compressor in
   * one go. You can use `0` or `Int.MaxValue` to disable the buffer completely.
   *
   * In general, it is recommended to turn off the buffer and prevent generation of overlong chunks at the source.
   */
  def gzip(
      bufferSize: Int = 512,
      compressionLevel: Int = Deflater.DEFAULT_COMPRESSION
  ): Flow[ByteString, ByteString, _] = {
    Flow[ByteString]
      .via(chunkerIfNeeded(bufferSize))
      .via(Compression.gzip(compressionLevel))
  }

  private def chunkerIfNeeded(bufferSize: Int): Flow[ByteString, ByteString, Any] =
    if (bufferSize > 0 || bufferSize < Int.MaxValue) Flow.fromGraph(new Chunker(bufferSize))
    else Flow[ByteString]

  // http://doc.akka.io/docs/akka/2.4.14/scala/stream/stream-cookbook.html#Chunking_up_a_stream_of_ByteStrings_into_limited_size_ByteStrings
  private class Chunker(val chunkSize: Int) extends GraphStage[FlowShape[ByteString, ByteString]] {
    private val in  = Inlet[ByteString]("Chunker.in")
    private val out = Outlet[ByteString]("Chunker.out")

    override val shape: FlowShape[ByteString, ByteString] = FlowShape.of(in, out)
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      private var buffer = ByteString.empty

      setHandler(out, new OutHandler {
        override def onPull(): Unit = emitChunk()
      })
      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            val elem = grab(in)
            buffer ++= elem
            emitChunk()
          }

          override def onUpstreamFinish(): Unit = {
            if (buffer.isEmpty) completeStage()
            else {
              // There are elements left in buffer, so
              // we keep accepting downstream pulls and push from buffer until emptied.
              //
              // It might be though, that the upstream finished while it was pulled, in which
              // case we will not get an onPull from the downstream, because we already had one.
              // In that case we need to emit from the buffer.
              if (isAvailable(out)) emitChunk()
            }
          }
        }
      )

      private def emitChunk(): Unit = {
        if (buffer.isEmpty) {
          if (isClosed(in)) completeStage()
          else pull(in)
        } else {
          val (chunk, nextBuffer) = buffer.splitAt(chunkSize)
          buffer = nextBuffer
          push(out, chunk)
        }
      }
    }
  }
}
