/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt.AutoPlugin
import sbt.Keys._
import sbt.ThisBuild

import Omnidoc.autoImport.omnidocGithubRepo
import Omnidoc.autoImport.omnidocTagPrefix

/**
 * Base Plugin for Play libraries.
 *
 * - Includes omnidoc configuration
 * - Cross builds the project
 */
object PlayLibraryBase extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = PlayBuildBase && Omnidoc

  import PlayBuildBase.autoImport._

  override def projectSettings = Seq(
    omnidocGithubRepo := s"playframework/${(ThisBuild / playBuildRepoName).value}",
    omnidocTagPrefix  := "",
    compile / javacOptions ++= Seq("--release", "17"),
    doc / javacOptions := Seq("-source", "17"),
    scalaVersion       := ScalaVersions.resolveScalaVersion(
      sys.props.getOrElse("scala.version", ScalaVersions.scala213Version)
    ),
    crossScalaVersions := ScalaVersions.publishedScalaVersions,
    scalacOptions ++= {
      if (scalaVersion.value.startsWith("3.3.")) Seq("-Yfuture-lazy-vals") else Seq.empty
    },
  )
}
