/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package com.typesafe.play.docs.sbtplugin

import sbt._
import sbt.compiler.Eval

private[sbtplugin] trait PlayDocsPluginCompat {

  def defaultLoad(state: State, localBase: java.io.File): (() => Eval, BuildStructure) = {
    Load.defaultLoad(state, localBase, state.log)
  }

  def getRelativeFile(baseDirectory: java.io.File, sbtFile: java.io.File): String = {
    relativeTo(baseDirectory)(sbtFile).getOrElse(sbtFile.getAbsolutePath)
  }

  def evaluateConfigurations(sbtFile: java.io.File, imports: Seq[String], classLoader: ClassLoader, eval: () => Eval): Seq[Def.Setting[_]] = {
    EvaluateConfigurations.evaluateConfiguration(eval(), sbtFile, imports)(classLoader)
  }
}
