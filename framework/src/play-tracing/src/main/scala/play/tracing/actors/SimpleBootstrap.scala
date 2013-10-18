/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

import akka.actor.{ ActorContext, ActorRef, ActorSystem }
import com.typesafe.config.Config
import play.tracing.analyzers.{ RequestAverager => RA, RequestAccumulator }
import akka.pattern.ask
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.Duration
import akka.util.Timeout

trait SimpleBootstrap {
  def actorSystemName: String
  def config: Config
  def analyzerSystemName: String
  implicit def askTimeout: Timeout
  implicit def askDuration: Duration = askTimeout.duration

  def builders(context: ActorContext): ActorBuilders = new ActorBuilders {
    def collector(processors: ActorRef): ActorRef =
      context.actorOf(Collector.props(processors))

    def publishers(): ActorRef =
      context.actorOf(RequestAverageLogPublisher.props())

    def processors(publishers: ActorRef): ActorRef = {
      context.actorOf(RequestAveragerActor.props({ (sender: ActorRef, state: RequestAccumulator[RA]) =>
        val s = state.snapshot
        if (s.totalCount > 0) {
          publishers.tell(RequestAverageLogPublisher.PublishRequestAverage(state.snapshot.averages), sender)
          sender ! Analyzer.Published
        }
      }))
    }

    def shutdownPublishers(publishers: ActorRef): Unit = {
      val f: Future[Publisher.ShutdownCompleted.type] = (publishers ask Publisher.Shutdown).mapTo[Publisher.ShutdownCompleted.type]
      Await.result(f, askDuration)
    }

    def shutdownProcessors(analyzers: ActorRef): Unit = {
      val f: Future[Processor.ShutdownCompleted.type] = (analyzers ask Processor.Shutdown).mapTo[Processor.ShutdownCompleted.type]
      Await.result(f, askDuration)
    }

    def shutdownCollector(collector: ActorRef): Unit = {
      val f: Future[Collector.ShutdownCompleted.type] = (collector ask Collector.Shutdown).mapTo[Collector.ShutdownCompleted.type]
      Await.result(f, askDuration)
    }
  }

  lazy val system: ActorSystem = ActorSystem(actorSystemName, config)
  lazy val analyzerSystem: ActorRef = system.actorOf(AnalyzerSystem.props(builders))

  def shutdown(): Unit = {
    analyzerSystem ! AnalyzerSystem.Shutdown
  }

}
