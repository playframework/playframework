name := "play-java"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "%SCALA_VERSION%"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)
