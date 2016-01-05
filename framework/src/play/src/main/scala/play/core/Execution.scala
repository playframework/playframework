/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import java.util.concurrent.ForkJoinPool
import play.api.{ Application, Play }
import scala.concurrent.ExecutionContext

/**
 * Provides access to Play's internal ExecutionContext.
 */
private[play] object Execution {

  def internalContext: ExecutionContext = {
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