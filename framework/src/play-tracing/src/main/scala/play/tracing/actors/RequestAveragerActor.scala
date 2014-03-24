/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

import play.tracing.tracers.SimpleTracer.TraceSummary
import play.tracing.analyzers.{ RequestAverager, RequestAccumulator }
import akka.actor.{ Cancellable, Props, ActorRef }
import scala.concurrent.duration._

trait RequestAveragerLike extends AnalyzerLike[TraceSummary, RequestAccumulator[RequestAverager], RequestAverager] {
  import Processor._, Analyzer._, TraceSummaryProcessor._
  def initialValue: RequestAverager
  val stateHolder: RequestAccumulator[RequestAverager] = new RequestAccumulator[RequestAverager](initialValue)

  final val dataExtractor: UnapplyHelper[ProcessTraceSummary] = TraceSummaryProcessor.dataExtractor

  final val stateExtractor: UnapplyHelper[ResetState[RequestAverager]] = new UnapplyHelper[ResetState[RequestAverager]] {
    def unapply(in: Any): Option[ResetState[RequestAverager]] = in match {
      case c @ ResetToEmptyState => Some(c)
      case _ => None
    }
  }

  def onResetState[S1 >: RequestAverager](command: ResetState[S1], sender: ActorRef, stateHolder: RequestAccumulator[RequestAverager]): Unit =
    respondTo(command, sender) {
      stateHolder.resetTo(RequestAverager())
      StateReset
    }

  def onProcess[A1 >: TraceSummary](command: Process[A1], sender: ActorRef, stateHolder: RequestAccumulator[RequestAverager]): Unit = command match {
    case ProcessTraceSummary(data) =>
      respondTo(command, sender) {
        stateHolder += data
        Processed
      }
  }

  def onShutdown(sender: ActorRef, stateHolder: RequestAccumulator[RequestAverager]): Unit =
    respondTo(Shutdown, sender) {
      onPublish(sender, stateHolder)
      ShutdownCompleted
    }
}

object RequestAveragerActor {
  def props(doPublish: (ActorRef, RequestAccumulator[RequestAverager]) => Unit,
    selfPublishInterval: FiniteDuration = 30.seconds,
    initialValue: RequestAverager = RequestAverager()): Props =
    Props(classOf[RequestAveragerActor], doPublish, selfPublishInterval, initialValue)
}

class RequestAveragerActor(doPublish: (ActorRef, RequestAccumulator[RequestAverager]) => Unit,
    selfPublishInterval: FiniteDuration,
    val initialValue: RequestAverager) extends RequestAveragerLike {

  var selfPublishTick: Cancellable = _

  override def preStart(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    selfPublishTick = context.system.scheduler.schedule(selfPublishInterval, selfPublishInterval, self, Analyzer.Publish)
    super.preStart()
  }

  override def postStop(): Unit = {
    selfPublishTick.cancel()
    selfPublishTick = null
    super.postStop()
  }

  def onPublish(sender: ActorRef, stateHolder: RequestAccumulator[RequestAverager]): Unit =
    doPublish(sender, stateHolder)
}
