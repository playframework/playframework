/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import sbt.File

object Generators {
  // Generates a scala file that contains the play version for use at runtime.
  def PlayVersion(scalaVersion: String)(dir: File): Seq[File] = {
      val file = dir / "PlayVersion.scala"
      IO.write(file,
        """|package play.core
            |
            |object PlayVersion {
            |    val current = "%s"
            |    val scalaVersion = "%s"
            |    val sbtVersion = "%s"
            |}
          """.stripMargin.format(BuildSettings.buildVersion, scalaVersion, BuildSettings.buildSbtVersion))
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
