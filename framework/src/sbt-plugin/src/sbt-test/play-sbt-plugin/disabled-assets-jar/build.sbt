//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

import java.net.URLClassLoader
import com.typesafe.sbt.packager.Keys.executableScriptName

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "assets-sample",
    version := "1.0-SNAPSHOT",
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.3"),
    includeFilter in (Assets, LessKeys.less) := "*.less",
    excludeFilter in (Assets, LessKeys.less) := "_*.less",
    PlayKeys.generateAssetsJar := false
  )
