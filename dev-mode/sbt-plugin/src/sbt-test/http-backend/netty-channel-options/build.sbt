//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//
name := """netty-channel-options"""
organization := "com.lightbend.play"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(PlayNettyServer)
  .disablePlugins(PlayAkkaHttpServer)
  .settings(
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies += guice,
    InputKey[Unit]("callIndex") := ScriptedTools.callIndex(),
    InputKey[Unit]("checkLines") := {
      val args                  = Def.spaceDelimited("<source> <target>").parsed
      val source :: target :: _ = args
      ScriptedTools.checkLines(source, target)
    }
  )
