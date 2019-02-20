//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//
name := """netty-channel-options"""
organization := "com.lightbend.play"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(PlayNettyServer)
  .disablePlugins(PlayAkkaHttpServer)
  .settings(
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.8"),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies ++= Seq(
      guice
    ),
    InputKey[Unit]("callIndex") := {
      DevModeBuild.callIndex()
    }
  )
