name := "play-java"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion in ThisBuild := "%SCALA_VERSION%"

libraryDependencies ++= Seq(
  guice,
  javaJdbc,
  cache,
  javaWs,
  "com.h2database" % "h2" % "1.4.191" % Test
)
