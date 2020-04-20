//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

//#default
name := "foo"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  ehcache
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
//#default
