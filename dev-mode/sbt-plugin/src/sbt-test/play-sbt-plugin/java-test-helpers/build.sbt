// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

name := """java-test-helpers"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  // disable PlayLayoutPlugin because the `test` file used by `sbt-scripted` collides with the `test/` Play expects.
  .disablePlugins(PlayLayoutPlugin)

scalaVersion := ScriptedTools.scalaVersionFromJavaProperties()
updateOptions := updateOptions.value.withLatestSnapshots(false)
update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))

libraryDependencies += guice
