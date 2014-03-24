/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

import akka.actor.{ Props, ActorContext, Actor, ActorRef }
import scala.concurrent.{ ExecutionContext, Future }
import ExecutionContext.Implicits.global

trait ActorBuilders {
  def collector(processors: ActorRef): ActorRef
  def processors(publishers: ActorRef): ActorRef
  def publishers(): ActorRef
  def shutdownCollector(collector: ActorRef): Unit
  def shutdownProcessors(processors: ActorRef): Unit
  def shutdownPublishers(publishers: ActorRef): Unit
}

object AnalyzerSystem extends HasCommands {
  def props(builders: ActorContext => ActorBuilders): Props =
    Props(classOf[AnalyzerSystem], builders)

  case object RequestCollector extends Command
  case object Shutdown extends Command

  case object ShutdownContinue

  case class Collector(ref: ActorRef) extends Event
  case object ShutdownCompleted extends Event
}

class AnalyzerSystem(builders: ActorContext => ActorBuilders) extends Actor {
  import AnalyzerSystem._

  protected def shutdownStep(originalSender: ActorRef, stages: List[ActorRef => Future[Unit]]): Receive = stages match {
    case Nil =>
      {
        case ShutdownContinue =>
          originalSender ! ShutdownCompleted
          context.stop(self)
      }
    case h :: t =>
      {
        case ShutdownContinue =>
          h(self)
          context.become(shutdownStep(originalSender, t))
      }
  }

  protected def continueShutdown(body: => Unit)(self: ActorRef): Future[Unit] = {
    val r = Future(body)
    r.onComplete(_ => self ! ShutdownContinue)
    r
  }

  protected def setupShutdown: List[ActorRef => Future[Unit]] =
    List(continueShutdown(actorBuilders.shutdownCollector(collector)),
      continueShutdown(actorBuilders.shutdownProcessors(processors)),
      continueShutdown(actorBuilders.shutdownPublishers(publishers)))

  lazy val actorBuilders = builders(context)
  lazy val publishers = actorBuilders.publishers()
  lazy val processors = actorBuilders.processors(publishers)
  lazy val collector = actorBuilders.collector(processors)

  def receive: Actor.Receive = {
    case RequestCollector => sender ! Collector(collector)
    case Shutdown =>
      context.become(shutdownStep(sender, setupShutdown))
  }
}
