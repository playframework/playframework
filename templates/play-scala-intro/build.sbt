name := "play-scala-intro"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "%SCALA_VERSION%"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "%PLAY_SLICK_VERSION%",
  "com.typesafe.play" %% "play-slick-evolutions" % "%PLAY_SLICK_VERSION%",
  "com.h2database" % "h2" % "1.4.177",
  specs2 % Test
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
