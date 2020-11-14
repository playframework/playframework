//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//
lazy val root = (project in file("."))
  .enablePlugins(RoutesCompiler)
  .settings(
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    sources in (Compile, routes) := Seq(baseDirectory.value / "a.routes", baseDirectory.value / "b.routes"),
    // turn off cross paths so that expressions don't need to include the scala version
    crossPaths := false
  )
