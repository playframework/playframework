/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.controllers {
  sealed trait TrampolineContextProvider {
    implicit def trampoline: play.api.libs.streams.Execution.trampoline.type = play.core.Execution.Implicits.trampoline
  }
}

package controllers {
  import play.api.controllers.TrampolineContextProvider

  object Execution extends TrampolineContextProvider
}
