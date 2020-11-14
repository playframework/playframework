/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

private[play] object Execution {
  def trampoline = play.api.libs.streams.Execution.trampoline

  object Implicits {
    implicit def trampoline = Execution.trampoline
  }
}
