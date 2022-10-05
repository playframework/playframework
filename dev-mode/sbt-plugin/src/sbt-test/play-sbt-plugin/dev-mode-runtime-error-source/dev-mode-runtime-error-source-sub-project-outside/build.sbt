//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val `sub-project-outside` = (project in file("."))
  .settings(commonSettings: _*)

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := ScriptedTools.scalaVersionFromJavaProperties(),
  updateOptions := updateOptions.value.withLatestSnapshots(false),
  update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
  // This makes it possible to run tests on the output regardless of scala version
  crossPaths := false
)
