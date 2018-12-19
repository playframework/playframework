//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//
name := """multiple-databases"""
organization := "com.lightbend.play"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    scalaVersion := "2.12.8",

    libraryDependencies ++= Seq(
      guice,
      javaJdbc,
      evolutions,
      "com.h2database" % "h2" % "1.4.197"
    ),

    InputKey[Unit]("verifyResourceContains") := {
      val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      DevModeBuild.verifyResourceContains(path, status.toInt, assertions, 0)
    }
  )

