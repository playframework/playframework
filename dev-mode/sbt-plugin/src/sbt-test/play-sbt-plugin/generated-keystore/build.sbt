// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayService)
  .settings(
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    libraryDependencies += guice,
    PlayKeys.playInteractionMode  := play.sbt.StaticPlayNonBlockingInteractionMode,
    InputKey[Unit]("makeRequest") := {
      val args                = Def.spaceDelimited("<path> <status> ...").parsed
      val path :: status :: _ = args
      ScriptedTools.verifyResourceContainsSsl(path, status.toInt)
    }
  )
