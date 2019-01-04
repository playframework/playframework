/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._
import sbt.Keys._
import sbt.complete.Parser

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
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepTask(bintrayRelease in thisProjectRef.value),
      releaseStepCommand("sonatypeRelease"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )

  /**
   * sbt release's releaseStepCommand does not execute remaining commands, which sbt-doge relies on
   */
  private def releaseStepCommandAndRemaining(command: String): State => State = { originalState =>
    // Capture current remaining commands
    val originalRemaining = originalState.remainingCommands

    def runCommand(command: String, state: State): State = {
      val newState = Parser.parse(command, state.combinedParser) match {
        case Right(cmd) => cmd()
        case Left(msg) => throw sys.error(s"Invalid programmatic input:\n$msg")
      }
      if (newState.remainingCommands.isEmpty) {
        newState
      } else {
        runCommand(newState.remainingCommands.head, newState.copy(remainingCommands = newState.remainingCommands.tail))
      }
    }

    runCommand(command, originalState.copy(remainingCommands = Nil)).copy(remainingCommands = originalRemaining)
  }
}
