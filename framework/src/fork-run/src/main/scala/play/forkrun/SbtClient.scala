/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.forkrun

import akka.actor._
import java.io.File
import sbt.client.actors.{ SbtClientProxy, SbtConnectionProxy }
import sbt.client.{ SbtConnector, TaskKey }
import sbt.protocol.{ ScopedKey, TaskResult, TaskFailure }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Success, Failure }

object SbtClient {
  sealed trait SbtRequest
  case class Execute(input: String) extends SbtRequest
  case class Request(key: String, sendTo: ActorRef) extends SbtRequest
  case class Response(key: String, result: TaskResult)
  case class Failed(error: Throwable)
  case object Shutdown
  case object ShutdownTimeout

  def props(baseDirectory: File, log: Logger, logEvents: Boolean): Props = Props(new SbtClient(baseDirectory, log, logEvents))
}

class SbtClient(baseDirectory: File, log: Logger, logEvents: Boolean) extends Actor {
  import SbtClient._

  val connector = SbtConnector("play-fork-run", "Play Fork Run", baseDirectory)
  val connection = context.actorOf(SbtConnectionProxy.props(connector), "sbt-connection")

  def receive: Receive = init

  def init: Receive = {
    connection ! SbtConnectionProxy.NewClient(self)
    connecting()
  }

  def awaitingDaemon(client: ActorRef, pending: Seq[SbtRequest] = Seq.empty): Receive = {
    case Terminated(`client`) => shutdownWithClient(client)
    case SbtClientProxy.DaemonSet =>
      if (logEvents) {
        val events = context.actorOf(SbtEvents.props(log), "sbt-server-events")
        client ! SbtClientProxy.SubscribeToEvents(sendTo = events)
      }
      pending foreach self.!
      context become active(client)
    case request: SbtRequest =>
      context become awaitingDaemon(client, pending :+ request)
    case Shutdown => shutdownWithClient(client)
  }

  def connecting(pending: Seq[SbtRequest] = Seq.empty): Receive = {
    case SbtConnectionProxy.NewClientResponse.Connected(client) =>
      // The SbtClientProxy will close and terminate if there is a non-recoverable error
      context.watch(client)
      client ! SbtClientProxy.SetDaemon(true, self)
      context become awaitingDaemon(client, pending)
    case SbtConnectionProxy.NewClientResponse.Error(recoverable, error) =>
      if (!recoverable) fail(new Exception(error), pending)
    case request: SbtRequest =>
      context become connecting(pending :+ request)
    case Shutdown => shutdown()
  }

  def active(client: ActorRef): Receive = {
    case Terminated(`client`) => shutdownWithClient(client)
    case Execute(input) =>
      client ! SbtClientProxy.RequestExecution.ByCommandOrTask(input, interaction = None, sendTo = self)
    case request @ Request(key, sendTo) =>
      val name = java.net.URLEncoder.encode(key, "utf-8")
      val task = context.child(name) getOrElse context.actorOf(SbtTask.props(key, client), name)
      task ! request
    case Shutdown => shutdownWithClient(client)
  }

  def fail(error: Throwable, requests: Seq[SbtRequest]): Unit = {
    requests foreach { request =>
      request match {
        case Request(_, sendTo) => sendTo ! Failed(error)
        case _ => // ignore
      }
    }
    context become broken(error)
  }

  def broken(error: Throwable): Receive = {
    case request: Request => request.sendTo ! Failed(error)
    case Shutdown => shutdown()
  }

  def shutdownWithClient(client: ActorRef): Unit = {
    context.unwatch(client)
    shutdown()
  }

  def shutdown(): Unit = {
    connection ! SbtConnectionProxy.Close(self)
    context become exiting
  }

  def exiting: Receive = {
    // can only wait so long - set up race.
    context.system.scheduler.scheduleOnce(10.seconds, self, ShutdownTimeout)

    {
      case SbtConnectionProxy.Closed | ShutdownTimeout => context.system.shutdown()
    }
  }
}

object SbtEvents {
  def props(logger: Logger): Props = Props(new SbtEvents(logger))
}

class SbtEvents(logger: Logger) extends Actor {
  import sbt.protocol._

  def receive = {
    case TaskLogEvent(id, LogMessage(level, message)) =>
      if (accepted(message)) logger.log(level, message)
  }

  // log events from sbt server currently have duplicates that are
  // taken from standard out and prefixed with "Read from stdout: "
  def accepted(message: String): Boolean = !(message startsWith "Read from stdout: ")
}

object SbtTask {
  def props(name: String, client: ActorRef): Props = Props(new SbtTask(name, client))
}

class SbtTask(name: String, client: ActorRef) extends Actor {
  import SbtClient.{ Request, Response, Failed }

  def receive: Receive = init

  def init: Receive = {
    client ! SbtClientProxy.LookupScopedKey(name, self)
    connecting()
  }

  def connecting(pending: Seq[Request] = Seq.empty): Receive = {
    case request: Request =>
      context become connecting(pending :+ request)
    case SbtClientProxy.LookupScopedKeyResponse(name, Success(keys)) =>
      if (keys.isEmpty) fail(new Exception(s"No sbt key found for $name"), pending)
      else client ! SbtClientProxy.WatchTask(TaskKey(keys.head), self)
    case SbtClientProxy.LookupScopedKeyResponse(name, Failure(error)) =>
      fail(error, pending)
    case SbtClientProxy.WatchingTask(taskKey) =>
      pending foreach self.!
      context become active(taskKey.key)
  }

  def active(key: ScopedKey, requests: Seq[Request] = Seq.empty): Receive = {
    case request: Request =>
      if (requests.isEmpty) client ! SbtClientProxy.RequestExecution.ByScopedKey(key, interaction = None, sendTo = self)
      context become active(key, requests :+ request)
    case SbtClientProxy.ExecutionId(Success(tid), _) => // ignore
    case SbtClientProxy.ExecutionId(Failure(error), _) =>
      fail(error, requests)
    case SbtClientProxy.WatchEvent(key, result) =>
      requests foreach (_.sendTo ! Response(name, result))
      context become active(key)
  }

  def fail(error: Throwable, requests: Seq[Request]): Unit = {
    requests foreach (_.sendTo ! Failed(error))
    context become broken(error)
  }

  def broken(error: Throwable): Receive = {
    case request: Request => request.sendTo ! Failed(error)
  }
}
