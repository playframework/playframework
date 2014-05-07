/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import sbt.File

object Generators {
  // Generates a scala file that contains the play version for use at runtime.
  def PlayVersion(dir: File): Seq[File] = {
      val file = dir / "PlayVersion.scala"
      IO.write(file,
        s"""|package play.core
            |
            |object PlayVersion {
            |    /** The current version of Play */
            |    val current = "${BuildSettings.buildVersion}"
            |    /** The version of Scala that Play was built against */
            |    val scalaVersion = "${BuildSettings.buildScalaVersion}"
            |    /** The default version of Scala to suggest for new projects */
            |    val defaultRuntimeScalaVersion = "${BuildSettings.defaultRuntimeScalaVersion}"
            |    val sbtVersion = "${BuildSettings.buildSbtVersion}"
            |}""".stripMargin)
      Seq(file)
  }
}

object Tasks {

  def scalaTemplateSourceMappings = (excludeFilter in unmanagedSources, unmanagedSourceDirectories in Compile, baseDirectory) map {
    (excludes, sdirs, base) =>
      val scalaTemplateSources = sdirs.descendantsExcept("*.scala.html", excludes)
      ((scalaTemplateSources --- sdirs --- base) pair (relativeTo(sdirs) | relativeTo(base) | flat)) toSeq
  }

}