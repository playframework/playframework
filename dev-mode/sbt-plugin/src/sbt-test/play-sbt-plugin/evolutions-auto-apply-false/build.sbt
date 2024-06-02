//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//
name := """auto-apply-false"""
organization := "com.lightbend.play"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies ++= Seq(guice, javaJdbc, evolutions, "com.h2database" % "h2" % "2.2.224"),
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
