// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(common: _*)
  .settings(
    name := "asset-changes-main",
    InputKey[Unit]("verifyResourceContains") := {
      val args                         = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      ScriptedTools.verifyResourceContains(path, status.toInt, assertions)
    },
  )
  .dependsOn(subproj)
  .aggregate(subproj)

lazy val subproj = (project in file("subproj"))
  .enablePlugins(PlayScala)
  .settings(common: _*)
  .settings(
    name := "asset-changes-sub",
  )

def common: Seq[Setting[?]] = Seq(
  version                      := "1.0-SNAPSHOT",
  PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
  scalaVersion                 := ScriptedTools.scalaVersionFromJavaProperties(),
  updateOptions                := updateOptions.value.withLatestSnapshots(false),
  update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
  libraryDependencies += guice,
)
Global / resolvers += "scala-integration".at("https://scala-ci.typesafe.com/artifactory/scala-integration/")
