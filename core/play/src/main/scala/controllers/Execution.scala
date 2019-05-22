/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.controllers {
  sealed trait TrampolineContextProvider {
    implicit def trampoline = play.core.Execution.Implicits.trampoline
  }
}

package controllers {

  import play.api.controllers.TrampolineContextProvider

  object Execution extends TrampolineContextProvider

}
