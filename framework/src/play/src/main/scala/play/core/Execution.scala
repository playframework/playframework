/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import java.util.concurrent.ForkJoinPool
import play.api.Play
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

/**
 * Provides access to Play's internal ExecutionContext.
 */
private[play] object Execution {

  /**
   * @deprecated Use the application execution context.
   * @return the actorsystem's execution context
   */
  @deprecated("Use an injected execution context", "2.6.0")
  def internalContext: ExecutionContextExecutor = {
    Play.privateMaybeApplication match {
      case None => common
      case Some(app) => app.actorSystem.dispatcher
    }
  }

  def trampoline = play.api.libs.streams.Execution.trampoline

  object Implicits {
    implicit def trampoline = Execution.trampoline
  }

  /**
   * Use this as a fallback when the application is unavailable.
   * The ForkJoinPool implementation promises to create threads on-demand
   * and clean them up when not in use (standard is when idle for 2
   * seconds).
   */
  private val common = ExecutionContext.fromExecutor(new ForkJoinPool())

}
