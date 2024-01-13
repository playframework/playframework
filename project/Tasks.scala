/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

import sbt.Keys._
import sbt._

object Generators {
  // Generates a scala file that contains the play version for use at runtime.
  def PlayVersion(
      version: String,
      scalaVersion: String,
      sbtVersion: String,
      jettyAlpnAgentVersion: String,
      akkaVersion: String,
      akkaHttpVersion: String,
      dir: File
  ): Seq[File] = {
    val file = dir / "PlayVersion.scala"
    val scalaSource =
      s"""|package play.core
          |
          |object PlayVersion {
          |  val current = "$version"
          |  val scalaVersion = "$scalaVersion"
          |  val sbtVersion = "$sbtVersion"
          |  val akkaVersion = "$akkaVersion"
          |  val akkaHttpVersion = "$akkaHttpVersion"
          |  @deprecated("2.8.4", "The Jetty ALPN Agent is not required for JDK8 after u252 and will be removed")
          |  private[play] val jettyAlpnAgentVersion = "$jettyAlpnAgentVersion"
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
        (packageDoc / publishArtifact in GlobalScope)(GlobalScope / publishArtifact) := toggle,
        (packageSrc / publishArtifact in GlobalScope)(GlobalScope / publishArtifact) := toggle,
        (GlobalScope / publishArtifact)               := true
      ),
      state.put(quickPublishToggle, toggle)
    )
  }
}
