/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.tracers

import java.util.concurrent.ConcurrentLinkedQueue
import play.instrumentation.spi.{ PlayHttpVersion, PlayHttpMethod, PlayHttpResponseStatus }
import play.core.utils.Instrumentation._
import scala.concurrent.duration._
import com.eaio.uuid.UUID

object SimpleTracer {
  case class TraceEvent(annotation: Annotation, timestamp: Long = System.nanoTime())

  case class Span(start: Long, end: Long, unit: TimeUnit = NANOSECONDS) {
    final val duration: Duration = FiniteDuration(end - start, unit)
  }

  case class HttpByteCount(header: Long, body: Long) {
    final val total: Long = header + body
  }

  trait TraceSummaryLike {
    def totalSpan: Span
    def inputProcessingSpan: Span
    def outputProcessingSpan: Span
    def actionProcessingSpan: Span
    def bytesIn: HttpByteCount
    def bytesOut: HttpByteCount
    def statusCode: PlayHttpResponseStatus
    def protocolVersion: PlayHttpVersion
    def controllerAndMethod: Option[String]
    def pattern: Option[String]
    def httpMethod: PlayHttpMethod
    def uri: String
  }

  case class TraceSummary(totalSpan: Span,
    inputProcessingSpan: Span,
    outputProcessingSpan: Span,
    actionProcessingSpan: Span,
    bytesIn: HttpByteCount,
    bytesOut: HttpByteCount,
    statusCode: PlayHttpResponseStatus,
    protocolVersion: PlayHttpVersion,
    controllerAndMethod: Option[String],
    pattern: Option[String],
    httpMethod: PlayHttpMethod,
    uri: String,
    uuid: UUID = new UUID()) extends TraceSummaryLike
}

class SimpleRawTraceResult(events: Array[SimpleTracer.TraceEvent]) {
  import SimpleTracer._
  final private class TraceSummaryBuilder(var totalSpan: Span = null,
      var inputProcessingSpan: Span = null,
      var outputProcessingSpan: Span = null,
      var actionProcessingSpan: Span = null,
      var bytesIn: HttpByteCount = null,
      var bytesOut: HttpByteCount = null,
      var statusCode: PlayHttpResponseStatus = null,
      var protocolVersion: PlayHttpVersion = null,
      var controllerAndMethod: Option[String] = null,
      var pattern: Option[String] = null,
      var httpMethod: PlayHttpMethod = null,
      var uri: String = null) extends TraceSummaryLike {
    def toTraceSummary(logger: String => Unit = println): Option[TraceSummary] = {
      def validSpan(s: Span): Boolean =
        s.start != 0L && s.end != 0L && s.start < s.end
      def validHttpByteCount(bc: HttpByteCount): Boolean =
        bc.header != 0L

      if (totalSpan != null && validSpan(totalSpan) &&
        inputProcessingSpan != null && validSpan(inputProcessingSpan) &&
        outputProcessingSpan != null && validSpan(outputProcessingSpan) &&
        actionProcessingSpan != null && validSpan(actionProcessingSpan) &&
        bytesIn != null && validHttpByteCount(bytesIn) &&
        bytesOut != null && validHttpByteCount(bytesOut) &&
        statusCode != null &&
        protocolVersion != null &&
        controllerAndMethod != null &&
        pattern != null &&
        httpMethod != null &&
        uri != null) Some(TraceSummary(totalSpan,
        inputProcessingSpan,
        outputProcessingSpan,
        actionProcessingSpan,
        bytesIn,
        bytesOut,
        statusCode,
        protocolVersion,
        controllerAndMethod,
        pattern,
        httpMethod,
        uri))
      else {
        if (totalSpan == null) logger("totalSpan == null")
        if (totalSpan != null && !validSpan(totalSpan)) logger("!validSpan(totalSpan)")
        if (inputProcessingSpan == null) logger("inputProcessingSpan == null")
        if (inputProcessingSpan != null && !validSpan(inputProcessingSpan)) logger(s"!validSpan(inputProcessingSpan): $inputProcessingSpan")
        if (outputProcessingSpan == null) logger("outputProcessingSpan == null")
        if (outputProcessingSpan != null && !validSpan(outputProcessingSpan)) logger(s"!validSpan(outputProcessingSpan) $outputProcessingSpan")
        if (actionProcessingSpan == null) logger("actionProcessingSpan == null")
        if (actionProcessingSpan != null && !validSpan(actionProcessingSpan)) logger(s"!validSpan(actionProcessingSpan) $actionProcessingSpan")
        if (bytesIn == null) logger("bytesIn == null")
        if (bytesIn != null && !validHttpByteCount(bytesIn)) logger("!validHttpByteCount(bytesIn)")
        if (bytesOut == null) logger("bytesOut == null")
        if (bytesOut != null && !validHttpByteCount(bytesOut)) logger("!validHttpByteCount(bytesOut)")
        if (statusCode == null) logger("statusCode == null")
        if (protocolVersion == null) logger("protocolVersion == null")
        if (controllerAndMethod == null) logger("controllerAndMethod == null")
        if (pattern == null) logger("pattern == null")
        if (httpMethod == null) logger("httpMethod == null")
        if (uri == null) logger("uri == null")
        None
      }
    }
  }

  final def dump(): Unit =
    events.toSeq.sortBy(_.timestamp).foreach(println)

  override def toString: String =
    s"""BasicRawTraceResult(${events.sortBy(_.timestamp).map(_.toString).mkString(", ")})"""

  // This function is deliberately written "long form" in order to generate a result in one pass.
  // Also, to minimize garbage generated (not eliminate)
  final def toSummary(logger: String => Unit = println): Option[TraceSummary] = {
    def updateSpanStart(span: Span, time: Long): Span =
      if (span == null) Span(time, 0L)
      else span.copy(start = time)
    def updateSpanEnd(span: Span, time: Long): Span =
      if (span == null) Span(0L, time)
      else span.copy(end = time)
    def updateHttpByteCountHeader(bc: HttpByteCount, bytes: Long): HttpByteCount =
      if (bc == null) HttpByteCount(bytes, 0L)
      else bc.copy(header = bc.header + bytes)
    def updateHttpByteCountBody(bc: HttpByteCount, bytes: Long): HttpByteCount =
      if (bc == null) HttpByteCount(0L, bytes)
      else bc.copy(body = bc.body + bytes)

    events.foldLeft(new TraceSummaryBuilder()) {
      (s, v) =>
        v.annotation match {
          case RequestStart => s.totalSpan = updateSpanStart(s.totalSpan, v.timestamp)
          case RequestEnd => s.totalSpan = updateSpanEnd(s.totalSpan, v.timestamp)
          case InputProcessingStart => s.inputProcessingSpan = updateSpanStart(s.inputProcessingSpan, v.timestamp)
          case InputProcessingEnd => s.inputProcessingSpan = updateSpanEnd(s.inputProcessingSpan, v.timestamp)
          case ActionStart => s.actionProcessingSpan = updateSpanStart(s.actionProcessingSpan, v.timestamp)
          case ActionEnd => s.actionProcessingSpan = updateSpanEnd(s.actionProcessingSpan, v.timestamp)
          case OutputProcessingStart => s.outputProcessingSpan = updateSpanStart(s.outputProcessingSpan, v.timestamp)
          case OutputProcessingEnd => s.outputProcessingSpan = updateSpanEnd(s.outputProcessingSpan, v.timestamp)
          case InputBodyBytes(bytes) => s.bytesIn = updateHttpByteCountBody(s.bytesIn, bytes)
          case InputHeader(header) =>
            s.bytesIn = updateHttpByteCountHeader(s.bytesIn, header.getHeaderSize)
            s.httpMethod = header.getMethod
            s.uri = header.getUri
            s.protocolVersion = header.getProtocolVersion
          case InputChunk(chunk) =>
            s.bytesIn = updateHttpByteCountBody(updateHttpByteCountHeader(s.bytesIn, chunk.getHeaderSize), chunk.getChunkSize)
          case OutputBodyBytes(bytes) => s.bytesOut = updateHttpByteCountBody(s.bytesOut, bytes)
          case OutputHeader(header) =>
            s.bytesOut = updateHttpByteCountHeader(s.bytesOut, header.getHeaderSize)
            s.statusCode = header.getStatus
          case OutputChunk(chunk) =>
            s.bytesOut = updateHttpByteCountBody(updateHttpByteCountHeader(s.bytesOut, chunk.getHeaderSize), chunk.getChunkSize)
          case Resolved(data) =>
            s.pattern = Some(data.getPath)
            s.controllerAndMethod = Some(data.getController + "." + data.getMethod)
          case _ =>
        }
        s
    }.toTraceSummary(logger)
  }
}

class SimpleTracer(val endHook: SimpleRawTraceResult => Unit) extends TracerLike[SimpleRawTraceResult] {
  import SimpleTracer._

  private final val queue: ConcurrentLinkedQueue[TraceEvent] = new ConcurrentLinkedQueue[TraceEvent]()
  private final def newEvent(event: Annotation): TraceEvent = TraceEvent(event)

  protected def genResult: SimpleRawTraceResult = new SimpleRawTraceResult(queue.toArray(new Array[TraceEvent](0)))
  protected final def recordInvocation(event: Annotation): Unit = queue.add(newEvent(event))
}
