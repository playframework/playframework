//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

import java.net.URLClassLoader
import com.typesafe.sbt.packager.Keys.executableScriptName

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "assets-sample",
    version := "1.0-SNAPSHOT",
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    Assets / LessKeys.less / includeFilter := "*.less",
    Assets / LessKeys.less / excludeFilter := "_*.less",
    PlayKeys.generateAssetsJar := false
  )
