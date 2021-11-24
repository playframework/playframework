//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val `sub-project-outside` = (project in file("."))
  .settings(commonSettings: _*)

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := sys.props("scala.version"),
  updateOptions := updateOptions.value.withLatestSnapshots(false),
  evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
  // This makes it possible to run tests on the output regardless of scala version
  crossPaths := false
)
