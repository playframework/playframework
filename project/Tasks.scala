/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import sbt.Keys._

object Generators {
  // Generates a scala file that contains the play version for use at runtime.
  def PlayVersion(
      version: String,
      scalaVersion: String,
      sbtVersion: String,
      pekkoVersion: String,
      pekkoHttpVersion: String,
      dir: File
  ): Seq[File] = {
    val file        = dir / "PlayVersion.scala"
    val scalaSource =
      s"""|package play.core
          |
          |object PlayVersion {
          |  val current = "$version"
          |  val scalaVersion = "$scalaVersion"
          |  val sbtVersion = "$sbtVersion"
          |  val pekkoVersion = "$pekkoVersion"
          |  val pekkoHttpVersion = "$pekkoHttpVersion"
          |}
          |""".stripMargin

    if (!file.exists() || IO.read(file) != scalaSource) {
      IO.write(file, scalaSource)
    }

    Seq(file)
  }
}

object Commands {
  val quickPublish = Command.command(
    "quickPublish",
    Help.more("quickPublish", "Toggles quick publish mode, disabling/enabling build of documentation/source jars")
  ) { state =>
    val projectExtract = Project.extract(state)
    import projectExtract._

    val quickPublishToggle = AttributeKey[Boolean]("quickPublishToggle")

    val toggle = !state.get(quickPublishToggle).getOrElse(true)

    val filtered = session.mergeSettings.filter { setting =>
      setting.key match {
        case Def.ScopedKey(Scope(_, Zero, Zero, Zero), key) if key == publishArtifact.key => false
        case other                                                                        => true
      }
    }

    if (toggle) {
      state.log.info("Turning off quick publish")
    } else {
      state.log.info("Turning on quick publish")
    }

    projectExtract.appendWithoutSession(
      filtered ++ Seq(
        GlobalScope / packageDoc / publishArtifact := toggle,
        GlobalScope / packageSrc / publishArtifact := toggle,
        GlobalScope / publishArtifact              := true
      ),
      state.put(quickPublishToggle, toggle)
    )
  }
}
