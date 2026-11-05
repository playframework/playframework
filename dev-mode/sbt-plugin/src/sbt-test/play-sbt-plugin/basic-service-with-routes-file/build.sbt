// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayService)
  .enablePlugins(RoutesCompiler)
  .settings(
    ScriptedTools.stableUniversalStagingDirectory,
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies += guice,
    InputKey[Unit]("verifyResourceContains") := {
      val args       = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path       = args.head
      val status     = args.tail.head.toInt
      val assertions = args.tail.tail
      ScriptedTools.verifyResourceContains(path, status, assertions)
    }
  )
