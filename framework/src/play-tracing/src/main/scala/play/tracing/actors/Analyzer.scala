/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

import akka.actor.{ ActorRef, Actor }

object Analyzer {
  import Processor._

  case object Publish extends Command
  trait ResetState[+S] extends Command

  case object StateReset extends Event
  case object Published extends Event
}

trait AnalyzerLike[+A, SH, +S] extends Actor {
  import Analyzer._, Processor._
  def stateHolder: SH
  def dataExtractor: UnapplyHelper[Process[A]]
  def stateExtractor: UnapplyHelper[ResetState[S]]
  def onProcess[A1 >: A](command: Process[A1], sender: ActorRef, stateHolder: SH): Unit
  def onPublish(sender: ActorRef, stateHolder: SH): Unit
  def onShutdown(sender: ActorRef, stateHolder: SH): Unit
  def onResetState[S1 >: S](command: ResetState[S1], sender: ActorRef, stateHolder: SH): Unit

  def receive: Actor.Receive = {
    val dataex: UnapplyHelper[Process[A]] = dataExtractor
    val stateex: UnapplyHelper[ResetState[S]] = stateExtractor

    {
      case `dataex`(x) =>
        onProcess(x, sender, stateHolder)
      case `stateex`(s) => onResetState(s, sender, stateHolder)
      case Publish => onPublish(sender, stateHolder)
      case Shutdown =>
        onShutdown(sender, stateHolder)
        context.stop(self)
      case Publisher.Published | Analyzer.Published => // Do nothing
    }
  }
}
