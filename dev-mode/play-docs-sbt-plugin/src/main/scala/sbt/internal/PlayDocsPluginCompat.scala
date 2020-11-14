/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
  ): Seq[Def.Setting[_]] = {
    EvaluateConfigurations.evaluateConfiguration(eval(), sbtFile, imports)(classLoader)
  }
}
