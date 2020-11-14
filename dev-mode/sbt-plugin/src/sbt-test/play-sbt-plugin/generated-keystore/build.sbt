//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayService)
  .settings(
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    libraryDependencies += guice,
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    InputKey[Unit]("makeRequest") := {
      val args                = Def.spaceDelimited("<path> <status> ...").parsed
      val path :: status :: _ = args
      ScriptedTools.verifyResourceContainsSsl(path, status.toInt)
    }
  )
