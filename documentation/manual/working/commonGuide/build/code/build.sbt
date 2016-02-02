//#default
name := "foo"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
//#default