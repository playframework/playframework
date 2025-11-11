// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

enablePlugins(PlayScala)

def Scala3 = "3.7.4"

scalaVersion := Scala3

crossScalaVersions := Seq("2.13.17", Scala3)

TaskKey[Unit]("check") := {
  val dir = crossTarget.value
  (Compile / managedSources).value.foreach { src =>
    assert(
      IO.relativize(dir, src).isDefined,
      s"scalaVersion = ${scalaVersion.value}, file = $src"
    )
  }
}
