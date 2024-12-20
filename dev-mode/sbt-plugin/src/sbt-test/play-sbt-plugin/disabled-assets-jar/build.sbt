// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import java.net.URLClassLoader

import com.typesafe.sbt.packager.Keys.executableScriptName

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name          := "assets-sample",
    version       := "1.0-SNAPSHOT",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    Assets / LessKeys.less / includeFilter := "*.less",
    Assets / LessKeys.less / excludeFilter := "_*.less",
    PlayKeys.generateAssetsJar             := false
  )
Global / resolvers += "scala-integration".at("https://scala-ci.typesafe.com/artifactory/scala-integration/")
