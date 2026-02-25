/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sbt.internal

import sbt._
import xsbti.FileConverter

object PlayDocsCompat:
  def runTask[T](key: TaskKey[T], state: State): Option[(State, Result[T])] =
    Project.extract(state).runTaskUnhandled(key, state)

  def getClasspathFiles(cp: Def.Classpath)(using fc: xsbti.FileConverter): Seq[java.io.File] =
    cp.files.map(_.toFile)
end PlayDocsCompat

trait PlayDocsPluginCompat {
  export sbt.Def.uncached

  def defaultLoad(state: State, localBase: java.io.File): (() => Any, BuildStructure) = {
    val extracted = Project.extract(state)
    (() => (), extracted.structure)
  }

  def evaluateConfigurations(
      sbtFile: java.io.File,
      imports: Seq[String],
      classLoader: ClassLoader,
      eval: () => Any
  ): Seq[Def.Setting[?]] = {
    Seq.empty
  }
}
