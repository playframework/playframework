/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

import akka.actor.{ ActorLogging, ActorRef, Actor, Props }
import play.tracing.analyzers.RequestAverage
import scala.concurrent.duration

object Publisher extends HasCommands {
  trait Publish[+T] extends Command
  case object Shutdown extends Command

  case object Published extends Event
  case object ShutdownCompleted extends Event
}

trait PublisherLike[T] extends Actor {
  import Publisher._
  def extractor: UnapplyHelper[Publish[T]]
  def onPublish(command: Publish[T], sender: ActorRef): Unit
  def onShutdown(sender: ActorRef): Unit

  def receive: Actor.Receive = {
    val ex: UnapplyHelper[Publish[T]] = extractor

    {
      case `ex`(x) => onPublish(x, sender)
      case Shutdown =>
        onShutdown(sender)
        context.stop(self)
    }
  }
}

object RequestAverageLogPublisher {
  import Publisher._

  case class PublishRequestAverage(data: RequestAverage) extends Publish[RequestAverage]
  def props(): Props = Props(classOf[RequestAverageLogPublisher])
}

class RequestAverageLogPublisher extends PublisherLike[RequestAverage] with ActorLogging {
  import Publisher._, RequestAverageLogPublisher._
  val extractor: UnapplyHelper[PublishRequestAverage] = new UnapplyHelper[PublishRequestAverage] {
    def unapply(in: Any): Option[PublishRequestAverage] = in match {
      case x: PublishRequestAverage => Some(x)
      case _ => None
    }
  }

  def onPublish(command: Publish[RequestAverage], sender: ActorRef): Unit = command match {
    case PublishRequestAverage(data) =>
      log.info(s"""
*****
request count: ${data.totalCount}
average duration: ${data.averageDuration.toUnit(duration.MILLISECONDS)} ms/request
average action duration: ${data.averageActionProcessingDuration.toUnit(duration.MILLISECONDS)} ms/request
average input duration: ${data.averageInputProcessingDuration.toUnit(duration.MILLISECONDS)} ms/request
average output duration: ${data.averageOutputProcessingDuration.toUnit(duration.MILLISECONDS)} ms/request
average bytes in: ${data.averageBodyBytesIn} bytes/request
average bytes out: ${data.averageBodyBytesOut} bytes/request
*****
""")
      sender ! Published
  }

  def onShutdown(sender: ActorRef): Unit = {
    sender ! ShutdownCompleted
  }
}
