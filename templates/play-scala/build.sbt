name := "play-scala"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion in ThisBuild := "%SCALA_VERSION%"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)
