/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

import akka.actor.{ ActorLogging, Props, ActorRef, Actor }
import play.tracing.tracers.SimpleRawTraceResult

object Collector extends HasCommands {
  case object Shutdown extends Command
  case object ShutdownCompleted extends Event

  def props(processors: ActorRef): Props =
    Props(classOf[Collector], processors)
}

class Collector(processors: ActorRef) extends Actor with ActorLogging {
  import Collector._

  def receive: Receive = {
    case x: SimpleRawTraceResult =>
      x.toSummary(log.debug).foreach(ts => processors ! TraceSummaryProcessor.ProcessTraceSummary(ts))
    case Shutdown =>
      context.stop(self)
      sender ! ShutdownCompleted
    case Processor.Processed =>
  }
}
