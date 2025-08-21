// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

enablePlugins(PlayScala)

def Scala3 = "3.7.2"

scalaVersion := Scala3

crossScalaVersions := Seq("2.13.16", Scala3)

scalacOptions += "-Werror"
