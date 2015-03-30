/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

import sbt._
import sbt.Keys._
import sbt.complete.Parsers

object Generators {
  // Generates a scala file that contains the play version for use at runtime.
  def PlayVersion(version: String, scalaVersion: String, sbtVersion: String, dir: File): Seq[File] = {
    val file = dir / "PlayVersion.scala"
    val scalaSource =
        """|package play.core
            |
            |object PlayVersion {
            |    val current = "%s"
            |    val scalaVersion = "%s"
            |    val sbtVersion = "%s"
            |}
          """.stripMargin.format(version, scalaVersion, sbtVersion)

    if (!file.exists() || IO.read(file) != scalaSource) {
      IO.write(file, scalaSource)
    }

    Seq(file)
  }
}

object Tasks {

  def scalaTemplateSourceMappings = (excludeFilter in unmanagedSources, unmanagedSourceDirectories in Compile, baseDirectory) map {
    (excludes, sdirs, base) =>
      val scalaTemplateSources = sdirs.descendantsExcept("*.scala.html", excludes)
      ((scalaTemplateSources --- sdirs --- base) pair (relativeTo(sdirs) | relativeTo(base) | flat)).toSeq
  }
}

object Commands {
  val quickPublish = Command("quickPublish", Help.more("quickPublish", "Toggles quick publish mode, disabling/enabling build of documentation/source jars"))(_ => Parsers.EOF) { (state, _) =>
    val x = Project.extract(state)
    import x._

    val quickPublishToggle = AttributeKey[Boolean]("quickPublishToggle")

    val toggle = !state.get(quickPublishToggle).getOrElse(true)

    val filtered = session.mergeSettings.filter { setting =>
      setting.key match {
         case Def.ScopedKey(Scope(_, Global, Global, Global), key)
           if key == publishArtifact.key => false
         case other => true
      }
    }

    if (toggle) {
      state.log.info("Turning off quick publish")
    } else {
      state.log.info("Turning on quick publish")
    }

    val newStructure = Load.reapply(filtered ++ Seq(
      publishArtifact in GlobalScope in packageDoc := toggle,
      publishArtifact in GlobalScope in packageSrc := toggle,
      publishArtifact in GlobalScope := true
    ), structure)
    Project.setProject(session, newStructure, state.put(quickPublishToggle, toggle))
  }
}
