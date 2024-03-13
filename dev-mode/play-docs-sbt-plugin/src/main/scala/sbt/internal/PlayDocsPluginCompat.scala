/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sbt.internal

import sbt._
import sbt.compiler.Eval

trait PlayDocsPluginCompat {
  def defaultLoad(state: State, localBase: java.io.File): (() => Eval, BuildStructure) = {
    Load.defaultLoad(state, localBase, state.log)
  }

  def evaluateConfigurations(
      sbtFile: java.io.File,
      imports: Seq[String],
      classLoader: ClassLoader,
      eval: () => Eval
  ): Seq[Def.Setting[?]] = {
    EvaluateConfigurations.evaluateConfiguration(eval(), sbtFile, imports)(classLoader)
  }
}
