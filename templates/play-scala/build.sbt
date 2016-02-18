name := "play-scala"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "%SCALA_VERSION%"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "%SCALATESTPLUS_PLAY_VERSION%" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
