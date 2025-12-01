/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import sbt.Keys._

object PlaySbtBuildBase extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = PlayBuildBase

  override def projectSettings = Seq(
    scalaVersion                  := ScalaVersions.scala212,
    crossScalaVersions            := Seq(ScalaVersions.scala212),
    pluginCrossBuild / sbtVersion := SbtVersions.sbt1,
    compile / javacOptions ++= Seq("--release", "11"),
    doc / javacOptions := Seq("-source", "11")
  )
}
