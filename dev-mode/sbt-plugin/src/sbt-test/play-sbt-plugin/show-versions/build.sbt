//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .settings(
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))
  )
