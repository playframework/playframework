/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

/**
 * Provides access to Play's internal ExecutionContext.
 */
private[play] object Execution {
  def trampoline = play.api.libs.streams.Execution.trampoline

  object Implicits {
    implicit def trampoline = Execution.trampoline
  }
}
