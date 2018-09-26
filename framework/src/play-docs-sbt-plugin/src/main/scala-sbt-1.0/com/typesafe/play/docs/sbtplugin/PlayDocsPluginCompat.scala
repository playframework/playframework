/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.docs.sbtplugin

import sbt._
import sbt.io.Path._
import sbt.compiler.Eval
import sbt.internal.{ BuildStructure, EvaluateConfigurations, Load }

private[sbtplugin] trait PlayDocsPluginCompat {

  def defaultLoad(state: State, localBase: java.io.File): (() => Eval, BuildStructure) = {
    sbt.internal.PlayLoad.defaultLoad(state, localBase)
  }

  def evaluateConfigurations(sbtFile: java.io.File, imports: Seq[String], classLoader: ClassLoader, eval: () => Eval): Seq[Def.Setting[_]] = {
    sbt.internal.PlayEvaluateConfigurations.evaluateConfigurations(sbtFile, imports, classLoader, eval)
  }
}
