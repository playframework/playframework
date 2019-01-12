//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//
name := """auto-apply-false"""
organization := "com.lightbend.play"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    scalaVersion := "2.12.8",
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,

    libraryDependencies ++= Seq(
      guice,
      javaJdbc,
      evolutions,
      "com.h2database" % "h2" % "1.4.197"
    ),

    InputKey[Unit]("applyEvolutions") := {
      val args = Def.spaceDelimited("<path>").parsed
      val path :: Nil = args
      DevModeBuild.applyEvolutions(path)
    },

    InputKey[Unit]("verifyResourceContains") := {
      val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      DevModeBuild.verifyResourceContains(path, status.toInt, assertions, 0)
    }
  )

