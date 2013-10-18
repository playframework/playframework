/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

import akka.actor.{ ActorRef, Actor }

object Processor extends HasCommands {
  trait Process[+T] extends Command
  case object Shutdown extends Command

  case object Processed extends Event
  case object ShutdownCompleted extends Event
}

trait ProcessorLike[+T] extends Actor {
  import Processor._
  def dataExtractor: UnapplyHelper[Process[T]]
  def onProcess[T1 >: T](command: Process[T1], sender: ActorRef): Unit
  def onShutdown(sender: ActorRef): Unit

  def receive: Actor.Receive = {
    val dataex: UnapplyHelper[Process[T]] = dataExtractor

    {
      case `dataex`(x) => onProcess(x, sender)
      case Shutdown =>
        onShutdown(sender)
        context.stop(self)
    }
  }
}
