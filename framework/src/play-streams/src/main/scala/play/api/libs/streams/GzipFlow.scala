/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams

import java.util.zip.GZIPOutputStream

import akka.stream.scaladsl.Flow
import akka.stream.stage._
import akka.stream._
import akka.util.ByteString

/**
 * A simple Gzip Flow
 *
 * GZIPs each chunk separately using the JDK7 syncFlush feature.
 */
object GzipFlow {

  /**
   * Create a Gzip Flow with the given buffer size.
   */
  def gzip(bufferSize: Int = 512): Flow[ByteString, ByteString, _] = {
    Flow[ByteString].via(new GzipStage(bufferSize))
  }

  private class GzipStage(bufferSize: Int) extends GraphStage[FlowShape[ByteString, ByteString]] {

    val in = Inlet[ByteString]("GzipStage.in")
    val out = Outlet[ByteString]("GzipStage.out")

    override val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with InHandler with OutHandler {

        val builder = ByteString.newBuilder
        // Uses syncFlush mode
        val gzipOs = new GZIPOutputStream(builder.asOutputStream, bufferSize, true)

        override def onPush(): Unit = {
          // For each chunk, we write it to the gzip output stream, flush which forces it to be entirely written to the
          // underlying ByteString builder, then we create the ByteString and clear the builder.
          val elem = grab(in)
          gzipOs.write(elem.toArray)
          gzipOs.flush()
          val result = builder.result()
          builder.clear()
          push(out, result)
        }

        override def onPull(): Unit = {
          // If finished, push the last ByteString
          if (isClosed(in)) {
            push(out, builder.result())
            completeStage()
          } else {
            // Otherwise request more demand from upstream
            pull(in)
          }
        }

        override def onUpstreamFinish() = {
          // Absorb termination, so we can send the last chunk out of the gzip output stream on the next pull
          gzipOs.close()
          if (isAvailable(out)) onPull()
        }

        override def postStop() = {
          // Close in case it's not already closed to release native deflate resources
          gzipOs.close()
          builder.clear()
        }

        setHandlers(in, out, this)
      }
  }

}