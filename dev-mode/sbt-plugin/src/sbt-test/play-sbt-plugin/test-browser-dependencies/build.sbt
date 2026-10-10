// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

// Applications get the test browser's dependencies (Selenide, Selenium's browser drivers, HtmlUnit) only from
// play-test-browser: play-test and play-specs2 declare them as optional dependencies.

ThisBuild / scalaVersion := ScriptedTools.scalaVersionFromJavaProperties()

lazy val withoutBrowser = (project in file("without-browser"))
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= Seq(guice, specs2 % Test))

lazy val withBrowser = (project in file("with-browser"))
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= Seq(guice, specs2 % Test, playTestBrowser % Test))
