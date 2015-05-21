/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import sbt.Keys._

import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import bintray.BintrayPlugin.autoImport._

object Release {

  val branchVersion = SettingKey[String]("branch-version", "The version to use if Play is on a branch.")

  def settings: Seq[Setting[_]] = Seq(
    // Disable cross building because we're using sbt-doge cross building
    releaseCrossBuild := false,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommand("+publishSigned"),
      releaseStepTask(bintrayRelease in thisProjectRef.value),
      releaseStepCommand("sonatypeRelease"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
}
