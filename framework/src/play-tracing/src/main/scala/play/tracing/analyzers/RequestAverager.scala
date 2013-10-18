/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.analyzers

import play.tracing.tracers.SimpleTracer.TraceSummary
import scala.concurrent.duration._

case class RequestAverage(totalCount: Long,
  averageDuration: Duration,
  averageInputProcessingDuration: Duration,
  averageOutputProcessingDuration: Duration,
  averageActionProcessingDuration: Duration,
  averageHeaderBytesIn: Double,
  averageBodyBytesIn: Double,
  averageHeaderBytesOut: Double,
  averageBodyBytesOut: Double)

case class RequestAverager(totalCount: Long = 0,
    totalDuration: Duration = Duration(0, NANOSECONDS),
    totalInputProcessingDuration: Duration = Duration(0, NANOSECONDS),
    totalOutputProcessingDuration: Duration = Duration(0, NANOSECONDS),
    totalActionProcessingDuration: Duration = Duration(0, NANOSECONDS),
    totalHeaderBytesIn: Long = 0L,
    totalBodyBytesIn: Long = 0L,
    totalHeaderBytesOut: Long = 0L,
    totalBodyBytesOut: Long = 0L) extends RequestAccumulatorLike[RequestAverager] {
  def +(summary: TraceSummary): RequestAverager =
    copy(this.totalCount + 1L,
      totalDuration + summary.totalSpan.duration,
      totalInputProcessingDuration + summary.inputProcessingSpan.duration,
      totalOutputProcessingDuration + summary.outputProcessingSpan.duration,
      totalActionProcessingDuration + summary.actionProcessingSpan.duration,
      totalHeaderBytesIn + summary.bytesIn.header,
      totalBodyBytesIn + summary.bytesIn.body,
      totalHeaderBytesOut + summary.bytesOut.header,
      totalBodyBytesOut + summary.bytesOut.body)

  def averages: RequestAverage = {
    val divisor = totalCount.toDouble
    RequestAverage(totalCount,
      totalDuration / divisor,
      totalInputProcessingDuration / divisor,
      totalOutputProcessingDuration / divisor,
      totalActionProcessingDuration / divisor,
      totalHeaderBytesIn / divisor,
      totalBodyBytesIn / divisor,
      totalHeaderBytesOut / divisor,
      totalBodyBytesOut / divisor)

  }
}
