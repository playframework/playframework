// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(commonSettings: _*)
  .dependsOn(a, c)
  .aggregate(common, a, b, c, nonplay)

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
  updateOptions := updateOptions.value.withLatestSnapshots(false),
  update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
  libraryDependencies += guice,
  routesGenerator := play.routes.compiler.InjectedRoutesGenerator,
  // This makes it possible to run tests on the output regardless of scala version
  crossPaths := false
)

lazy val common = (project in file("common"))
  .enablePlugins(PlayJava)
  .settings(commonSettings: _*)
  .settings(
    aggregateReverseRoutes := Seq(a, b, c)
  )

lazy val nonplay = (project in file("nonplay"))
  .settings(commonSettings: _*)

lazy val a: Project = (project in file("a"))
  .enablePlugins(PlayJava)
  .settings(commonSettings: _*)
  .dependsOn(nonplay, common)

lazy val b: Project = (project in file("b"))
  .enablePlugins(PlayJava)
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val c: Project = (project in file("c"))
  .enablePlugins(PlayJava)
  .settings(commonSettings: _*)
  .dependsOn(b)
Global / resolvers += "scala-integration".at("https://scala-ci.typesafe.com/artifactory/scala-integration/")
