// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

name := """auto-apply-false"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies ++= Seq(guice, javaJdbc, evolutions, "com.h2database" % "h2" % "2.3.232"),
    InputKey[Unit]("applyEvolutions") := {
      val args        = Def.spaceDelimited("<path>").parsed
      val path :: Nil = args
      ScriptedTools.applyEvolutions(path)
    },
    InputKey[Unit]("verifyResourceContains") := {
      val args                         = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      ScriptedTools.verifyResourceContains(path, status.toInt, assertions)
    }
  )
