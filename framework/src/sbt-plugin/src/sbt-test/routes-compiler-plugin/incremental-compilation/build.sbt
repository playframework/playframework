//
// Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
//
lazy val root = (project in file(".")).enablePlugins(RoutesCompiler)

scalaVersion := sys.props.get("scala.version").getOrElse("2.10.4")

sources in (Compile, routes) := Seq(baseDirectory.value / "a.routes", baseDirectory.value / "b.routes")

// turn off cross paths so that expressions don't need to include the scala version
crossPaths := false

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
