//
// Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
//
lazy val root = (project in file(".")).enablePlugins(RoutesCompiler)

scalaVersion := "2.11.1"

routesFiles in Compile := Seq(baseDirectory.value / "a.routes", baseDirectory.value / "b.routes")

// because the scripted newer command is broken:
// https://github.com/sbt/sbt/pull/1419
InputKey[Unit]("newer") := {
  val args = Def.spaceDelimited("<tocheck> <target>").parsed
  val base: File = baseDirectory.value
  val toCheck = args(0)
  val targetFile = args(1)
  if ((base / toCheck).lastModified() <= (base / targetFile).lastModified()) {
    throw new RuntimeException(s"$toCheck is not newer than $targetFile")
  }
}
