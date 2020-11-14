//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//
name := """akka-fork-join-pool"""
organization := "com.lightbend.play"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies += guice,
    InputKey[Unit]("callIndex") := {
      try ScriptedTools.callIndex() catch { case e: java.net.ConnectException =>
        play.sbt.run.PlayRun.stop(state.value)
        throw e
      }
    },
    InputKey[Unit]("checkLines") := {
      val args                  = Def.spaceDelimited("<source> <target>").parsed
      val source :: target :: _ = args
        try ScriptedTools.checkLines(source, target) catch { case e: java.net.ConnectException =>
          play.sbt.run.PlayRun.stop(state.value)
          throw e
        }
    }
  )
