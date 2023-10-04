/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import sbt.io.IO
import sbt.Keys._

object Playdoc extends AutoPlugin {

  final val Docs = config("docs")

  object autoImport {
    val playdocDirectory = settingKey[File]("Base directory of play documentation")
    val playdocPackage   = taskKey[File]("Package play documentation")
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = noTrigger

  override def projectSettings =
    Defaults.packageTaskSettings(playdocPackage, playdocPackage / mappings) ++
      Seq(
        playdocDirectory := (ThisBuild / baseDirectory).value / "docs" / "manual",
        playdocPackage / mappings := {
          val base: File = playdocDirectory.value
          base.allPaths.pair(IO.relativize(base.getParentFile(), _))
        },
        playdocPackage / artifactClassifier := Some("playdoc"),
        playdocPackage / artifact ~= { _.withConfigurations(Vector(Docs)) }
      ) ++
      addArtifact(playdocPackage / artifact, playdocPackage)

}
