/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sbt

import sbt.util.LoggerContext

object LoggerCompat:
  def createLoggerContext(state: State): LoggerContext =
    LoggerContext()
end LoggerCompat