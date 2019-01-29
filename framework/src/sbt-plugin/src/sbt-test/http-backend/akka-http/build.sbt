name := """play-scala-seed"""
organization := "com.example"

version := "1.0-SNAPSHOT"

//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  // disable PlayLayoutPlugin because the `test` file used by `sbt-scripted` collides with the `test/` Play expects.
  .disablePlugins(PlayLayoutPlugin)

scalaVersion := "2.12.8"

libraryDependencies += guice
libraryDependencies += specs2
libraryDependencies += ws
