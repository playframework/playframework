/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.analyzers

import play.tracing.tracers.SimpleTracer.TraceSummary

trait RequestAccumulatorLike[This <: RequestAccumulatorLike[_]] {
  def +(summary: TraceSummary): This
}

class RequestAccumulator[S <: RequestAccumulatorLike[S]](initial: S) {
  private var internal: S = initial
  def `+=`(summary: TraceSummary): Unit = {
    internal = internal + summary
  }
  def snapshot: S = internal
  def resetToInitial(): Unit = {
    internal = initial
  }
  def resetTo(value: S): Unit = {
    internal = value
  }
}
