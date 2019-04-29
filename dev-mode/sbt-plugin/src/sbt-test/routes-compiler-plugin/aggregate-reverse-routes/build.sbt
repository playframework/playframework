//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(commonSettings: _*)
  .dependsOn(a, c)
  .aggregate(common, a, b, c, nonplay)

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := sys.props.get("scala.version").getOrElse("2.12.8"),
  libraryDependencies += guice,
  routesGenerator := play.routes.compiler.InjectedRoutesGenerator,
  // This makes it possible to run tests on the output regardless of scala version
  crossPaths := false
)

lazy val common = (project in file("common"))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(commonSettings: _*)
  .settings(
    aggregateReverseRoutes := Seq(a, b, c)
  )

lazy val nonplay = (project in file("nonplay"))
  .settings(commonSettings: _*)

lazy val a: Project = (project in file("a"))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(commonSettings: _*)
  .dependsOn(nonplay, common)

lazy val b: Project = (project in file("b"))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val c: Project = (project in file("c"))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(commonSettings: _*)
  .dependsOn(b)
