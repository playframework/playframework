// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

name         := """play-scala-seed"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  // disable PlayLayoutPlugin because the `test` file used by `sbt-scripted` collides with the `test/` Play expects.
  .disablePlugins(PlayLayoutPlugin)

scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties()
updateOptions := updateOptions.value.withLatestSnapshots(false)
update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))

libraryDependencies += guice
libraryDependencies += specs2
libraryDependencies += ws

// Tyrus is the reference implementation for Java Websocket API (JSR-356)
libraryDependencies += "org.glassfish.tyrus" % "tyrus-container-jdk-client" % "2.1.1" % Test
Global / resolvers += "scala-integration".at("https://scala-ci.typesafe.com/artifactory/scala-integration/")
