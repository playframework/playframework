name := "play-scala"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "%SCALA_VERSION%"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
