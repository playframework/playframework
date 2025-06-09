/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt.librarymanagement.SemanticSelector
import sbt.librarymanagement.VersionNumber
import sbt.AutoPlugin
import sbt.Keys._
import sbt.ThisBuild

import Omnidoc.autoImport.omnidocGithubRepo
import Omnidoc.autoImport.omnidocTagPrefix

/**
 * Base Plugin for Play libraries.
 *
 * - Publishes to sonatype
 * - Includes omnidoc configuration
 * - Cross builds the project
 */
object PlayLibraryBase extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = PlayBuildBase && PlaySonatypeBase && Omnidoc

  import PlayBuildBase.autoImport._

  override def projectSettings = Seq(
    omnidocGithubRepo := s"playframework/${(ThisBuild / playBuildRepoName).value}",
    omnidocTagPrefix  := "",
    compile / javacOptions ++= Seq("--release", "11"),
    doc / javacOptions := Seq("-source", "11"),
    crossScalaVersions := Seq(scalaVersion.value, ScalaVersions.scala3),
    scalaVersion       := (Seq(ScalaVersions.scala213, ScalaVersions.scala3)
      .filter(v =>
        SemanticSelector(sys.props.get("scala.version").getOrElse(ScalaVersions.scala213)).matches(VersionNumber(v))
      ) match {
      case Nil          => sys.error("Unable to detect scalaVersion!")
      case Seq(version) => version
      case multiple     =>
        sys.error(
          s"Multiple crossScalaVersions matched query '${sys.props("scala.version")}': ${multiple.mkString(", ")}"
        )
    }),
  )
}
