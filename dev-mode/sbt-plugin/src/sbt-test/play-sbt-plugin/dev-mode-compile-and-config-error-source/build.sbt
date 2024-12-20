// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies += guice,
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
  )
  .dependsOn(`sub-project-inside`, `sub-project-outside`)
  .aggregate(`sub-project-inside`, `sub-project-outside`)

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
  updateOptions := updateOptions.value.withLatestSnapshots(false),
  update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
  // This makes it possible to run tests on the output regardless of scala version
  crossPaths := false
)

lazy val `sub-project-inside` = (project in file("./modules/sub-project-inside"))
  .settings(commonSettings: _*)

lazy val `sub-project-outside` =
  ProjectRef(file("./dev-mode-compile-and-config-error-source-sub-project-outside"), "sub-project-outside")
Global / resolvers += "scala-integration".at("https://scala-ci.typesafe.com/artifactory/scala-integration/")
