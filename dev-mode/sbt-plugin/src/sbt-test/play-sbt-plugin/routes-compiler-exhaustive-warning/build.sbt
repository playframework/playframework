// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

enablePlugins(PlayScala)

val selectedScalaVersion = ScriptedTools.scalaVersionFromJavaProperties()

scalaVersion := selectedScalaVersion

crossScalaVersions := Seq("2.13.18", selectedScalaVersion).distinct

scalacOptions += "-Werror"
