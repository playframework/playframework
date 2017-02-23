/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core

import java.util.concurrent.ForkJoinPool
import play.api.{ Application, Play }
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

/**
 * Provides access to Play's internal ExecutionContext.
 */
private[play] object Execution {

  def internalContext: ExecutionContextExecutor = {
    val appOrNull: Application = Play._currentApp
    appOrNull match {
      case null => common
      case app: Application => app.actorSystem.dispatcher
    }
  }

  object Implicits {

    implicit def internalContext = Execution.internalContext

  }

  /**
   * Use this as a fallback when the application is unavailable.
   * The ForkJoinPool implementation promises to create threads on-demand
   * and clean them up when not in use (standard is when idle for 2
   * seconds).
   */
  private val common = ExecutionContext.fromExecutor(new ForkJoinPool())

}
