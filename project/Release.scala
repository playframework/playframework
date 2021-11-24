/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._
import sbt.Keys._
import sbt.complete.Parser

import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

object Release {
  val branchVersion = SettingKey[String]("branch-version", "The version to use if Play is on a branch.")

  def settings: Seq[Setting[_]] = Seq(
    // See https://www.scala-sbt.org/1.x/docs/Cross-Build.html#Note+about+sbt-release for details about
    // these settings. They are required to make sbt 1 and sbt-release (at least < 1.0.10) work together.
    crossScalaVersions := Nil,
    publish / skip := true,
    // Disable cross building because we're using sbt's native "+" cross-building
    releaseCrossBuild := false,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      runClean,
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepCommand("sonatypeBundleRelease"),
      pushChanges
    )
  )

  /**
   * sbt release's releaseStepCommand does not execute remaining commands, which sbt's native "+" cross-building relies on (TBC)
   */
  private def releaseStepCommandAndRemaining(command: String): State => State = { originalState =>
    // Capture current remaining commands
    val originalRemaining = originalState.remainingCommands

    def runCommand(command: String, state: State): State = {
      val newState = Parser.parse(command, state.combinedParser) match {
        case Right(cmd) => cmd()
        case Left(msg)  => throw sys.error(s"Invalid programmatic input:\n$msg")
      }
      if (newState.remainingCommands.isEmpty) {
        newState
      } else {
        runCommand(
          newState.remainingCommands.head.commandLine,
          newState.copy(remainingCommands = newState.remainingCommands.tail)
        )
      }
    }

    runCommand(command, originalState.copy(remainingCommands = Nil)).copy(remainingCommands = originalRemaining)
  }
}
