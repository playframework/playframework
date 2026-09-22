// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

enablePlugins(PlayScala)

val selectedScalaVersion = ScriptedTools.scalaVersionFromJavaProperties()
val scala213Version       = ScriptedTools.scalaVersionFromJavaProperties("2.13.x")

scalaVersion := selectedScalaVersion

crossScalaVersions := Seq(scala213Version, selectedScalaVersion).distinct

scalacOptions += "-Werror"
