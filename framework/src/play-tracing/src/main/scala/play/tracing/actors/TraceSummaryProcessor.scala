/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

import play.tracing.tracers.SimpleTracer.TraceSummary

object TraceSummaryProcessor {
  import Processor._, Analyzer._

  case class ProcessTraceSummary(value: TraceSummary) extends Process[TraceSummary]
  case object ResetToEmptyState extends ResetState[Nothing]

  final val dataExtractor: UnapplyHelper[ProcessTraceSummary] = new UnapplyHelper[ProcessTraceSummary] {
    def unapply(in: Any): Option[ProcessTraceSummary] = in match {
      case c: ProcessTraceSummary => Some(c)
      case x @ _ => None
    }
  }
}
