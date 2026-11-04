// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)

scalaVersion := ScriptedTools.scalaVersionFromJavaProperties()

// can't use test directory since scripted calls its script "test"
Test / sourceDirectory := baseDirectory.value / "tests"

Test / javaSource := baseDirectory.value / "tests"
