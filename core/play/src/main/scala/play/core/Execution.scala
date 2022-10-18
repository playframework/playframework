/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

private[play] object Execution {
  def trampoline = play.api.libs.streams.Execution.trampoline

  object Implicits {
    implicit def trampoline: play.api.libs.streams.Execution.trampoline.type = Execution.trampoline
  }
}
