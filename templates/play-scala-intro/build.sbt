name := "play-scala-intro"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "%SCALA_VERSION%"

libraryDependencies ++= Seq(
  guiceSupport,
  "com.typesafe.play" %% "play-slick" % "%PLAY_SLICK_VERSION%",
  "com.typesafe.play" %% "play-slick-evolutions" % "%PLAY_SLICK_VERSION%",
  "com.h2database" % "h2" % "1.4.191",
  "org.scalatestplus.play" %% "scalatestplus-play" % "%SCALATESTPLUS_PLAY_VERSION%" % Test
)

