/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.control.NonFatal

import org.apache.pekko.actor.Scheduler
import org.apache.pekko.Done
import play.api.http.websocket.CloseMessage

private[server] final class WebSocketGracefulShutdown {
  private val monitor = new AnyRef

  private var nextId          = 0L
  private var registrations   = Map.empty[Long, CloseMessage => Unit]
  private var shutdownMessage = Option.empty[CloseMessage]
  private val allClosed       = Promise[Done]()

  def register(close: CloseMessage => Unit): () => Unit = {
    val (id, closeImmediately) = monitor.synchronized {
      val id = nextId
      nextId += 1
      registrations += id -> close
      (id, shutdownMessage)
    }

    closeImmediately.foreach(invoke(id, close, _))
    () => unregister(id)
  }

  def shutdown(
      close: CloseMessage,
      timeout: FiniteDuration
  )(scheduler: Scheduler, executionContext: ExecutionContext): Future[Done] = {
    implicit val ec: ExecutionContext = executionContext

    val (callbacks, result) = monitor.synchronized {
      shutdownMessage match {
        case None =>
          shutdownMessage = Some(close)
          if (registrations.isEmpty) {
            allClosed.trySuccess(Done)
          }
          (registrations.toList, allClosed.future)
        case Some(_) =>
          (Nil, allClosed.future)
      }
    }

    callbacks.foreach { case (id, callback) => invoke(id, callback, close) }
    // Bound shutdown from its initiation rather than from each Close emission,
    // so a backpressured connection cannot extend coordinated shutdown.
    val timeoutResult = Promise[Done]()
    val timeoutTask   = scheduler.scheduleOnce(timeout)(timeoutResult.trySuccess(Done))
    result.onComplete(_ => timeoutTask.cancel())
    Future.firstCompletedOf(Seq(result, timeoutResult.future))
  }

  private def invoke(id: Long, callback: CloseMessage => Unit, close: CloseMessage): Unit = {
    try {
      callback(close)
    } catch {
      case NonFatal(_) => unregister(id)
    }
  }

  private def unregister(id: Long): Unit = monitor.synchronized {
    registrations -= id
    if (shutdownMessage.nonEmpty && registrations.isEmpty) {
      allClosed.trySuccess(Done)
    }
  }
}
