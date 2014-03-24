/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

import akka.actor.ActorRef

trait HasCommands {
  trait HasFailureMessage { this: Command =>
    def failureMessage(message: String): Event = CommandFailed(this, message)
  }

  sealed trait Message

  trait Command extends Message with HasFailureMessage

  trait Event extends Message
  case class CommandFailed(cmd: Command, message: String) extends Event

  def respondTo(command: Command, to: ActorRef)(body: => Event): Unit = {
    to ! (try { body }
    catch { case e: Exception => command.failureMessage(e.getMessage) })
  }
}
